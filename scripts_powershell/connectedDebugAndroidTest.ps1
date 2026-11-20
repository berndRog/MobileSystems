#!/usr/bin/env pwsh
#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# Inspect native exit codes explicitly, regardless of the caller's preferences.
$PSNativeCommandUseErrorActionPreference = $false

$scriptExitCode = 0
try {
    $repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
    $projects = @(Get-ChildItem -LiteralPath $repoRoot -Directory |
        Where-Object { $_.Name -cmatch '^A[0-9].*_.*$' } |
        Sort-Object -Property Name -CaseSensitive)
    $wrapperName = if ($IsWindows) { 'gradlew.bat' } else { 'gradlew' }
    $gradleWrapper = Join-Path $repoRoot $wrapperName
    if ($projects.Count -gt 0 -and -not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
        throw "Gradle-Wrapper fehlt: $gradleWrapper"
    }

    function Invoke-AndroidTests {
        param(
            [string] $SourceDirectory,
            [string] $ProjectDirectory,
            [string] $Task
        )

        $name = Split-Path -Leaf $SourceDirectory
        [Console]::WriteLine("Android-Tests: {0}", $name)
        $sourceRoot = Join-Path $SourceDirectory 'src/androidTest'
        $hasTests = $false
        if (Test-Path -LiteralPath $sourceRoot -PathType Container) {
            $testSource = Get-ChildItem -LiteralPath $sourceRoot -Recurse -File |
                Where-Object { $_.Extension -in '.kt', '.java' } |
                Select-Object -First 1
            $hasTests = $null -ne $testSource
        }
        if (-not $hasTests) {
            [Console]::WriteLine("Übersprungen (keine Android-Tests): {0}", $name)
            return
        }

        $resultsDirectory = Join-Path $SourceDirectory 'build/outputs/androidTest-results/connected/debug'
        if (Test-Path -LiteralPath $resultsDirectory) {
            Remove-Item -LiteralPath $resultsDirectory -Recurse -Force
        }

        & $gradleWrapper '-p' $ProjectDirectory $Task '--quiet' '--warning-mode=none'
        $gradleExitCode = $LASTEXITCODE
        if ($gradleExitCode -ne 0) {
            $script:scriptExitCode = $gradleExitCode
            throw "Gradle fehlgeschlagen für $name (Exit-Code $gradleExitCode)."
        }

        $result = $null
        if (Test-Path -LiteralPath $resultsDirectory -PathType Container) {
            # Match the original's maxdepth 1: nested or stale reports do not count.
            $result = Get-ChildItem -LiteralPath $resultsDirectory -File -Filter 'TEST-*.xml' |
                Select-Object -First 1
        }
        if ($null -eq $result) {
            throw "Keine Testergebnisse für $name; Installation und Geräteverbindung prüfen."
        }
        [Console]::WriteLine("OK: {0}", $name)
    }

    $sharedTested = $false
    foreach ($project in $projects) {
        if (-not $sharedTested) {
            Invoke-AndroidTests -SourceDirectory (Join-Path $repoRoot 'Shared') -ProjectDirectory $project.FullName -Task ':Shared:connectedDebugAndroidTest'
            $sharedTested = $true
        }
        Invoke-AndroidTests -SourceDirectory $project.FullName -ProjectDirectory $project.FullName -Task ':connectedDebugAndroidTest'
    }

}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    if ($scriptExitCode -eq 0) { $scriptExitCode = 1 }
}
exit $scriptExitCode
