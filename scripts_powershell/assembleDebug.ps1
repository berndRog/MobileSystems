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

    # Remove only the apps' local configuration caches, as in the Bash version.
    foreach ($project in $projects) {
        $cache = Join-Path $project.FullName '.gradle/configuration-cache'
        if (Test-Path -LiteralPath $cache) {
            Remove-Item -LiteralPath $cache -Recurse -Force
        }
    }

    foreach ($project in $projects) {
        [Console]::WriteLine("Build: {0}", $project.Name)
        & $gradleWrapper '-p' $project.FullName 'assembleDebug' '--quiet' '--warning-mode=none'
        $gradleExitCode = $LASTEXITCODE
        if ($gradleExitCode -ne 0) {
            $scriptExitCode = $gradleExitCode
            throw "Gradle fehlgeschlagen für $($project.Name) (Exit-Code $gradleExitCode)."
        }
        [Console]::WriteLine("OK: {0}", $project.Name)
    }

}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    if ($scriptExitCode -eq 0) { $scriptExitCode = 1 }
}
exit $scriptExitCode
