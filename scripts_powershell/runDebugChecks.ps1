#!/usr/bin/env pwsh
#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

$scriptExitCode = 0
$logFile = $null
$stopwatch = $null
try {
    $repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
    $scripts = @('assembleDebug.ps1', 'testDebugUnitTest.ps1', 'connectedDebugAndroidTest.ps1')
    $durations = @($null, $null, $null)
    $statuses = @('Nicht ausgeführt', 'Nicht ausgeführt', 'Nicht ausgeführt')

    foreach ($scriptName in $scripts) {
        $path = Join-Path $PSScriptRoot $scriptName
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Skript fehlt oder ist nicht lesbar: $path"
        }
        # Check readability before any build, not only when its turn is reached.
        $stream = [IO.File]::OpenRead($path)
        $stream.Dispose()
    }

    $logDirectory = Join-Path $repoRoot 'logs'
    [void] [IO.Directory]::CreateDirectory($logDirectory)
    $logFile = Join-Path $logDirectory ("debug-checks-{0}-{1}.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $PID)
    $utf8 = [Text.UTF8Encoding]::new($false)
    $startedAt = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'
    $stopwatch = [Diagnostics.Stopwatch]::StartNew()
    $shellExecutable = (Get-Process -Id $PID).Path

    function Write-LogLine {
        param([string] $Line)
        # Console and file I/O also work from finally after pipeline cancellation.
        [Console]::WriteLine($Line)
        [IO.File]::AppendAllText($logFile, $Line + [Environment]::NewLine, $utf8)
    }

    function Format-Duration {
        param([long] $Seconds)
        $hours = [long] [Math]::Floor($Seconds / 3600)
        $minutes = [long] [Math]::Floor(($Seconds % 3600) / 60)
        return ('{0:00}:{1:00}:{2:00}' -f $hours, $minutes, ($Seconds % 60))
    }

    for ($i = 0; $i -lt $scripts.Count; $i++) {
        Write-LogLine ''
        Write-LogLine ("Starte: {0}" -f $scripts[$i])
        $stepStarted = [long] [Math]::Floor($stopwatch.Elapsed.TotalSeconds)
        $statuses[$i] = 'Abgebrochen'
        # A cancelled native child must not be reported as success.
        $scriptExitCode = 130
        try {
            # Separate processes keep a child's exit from ending this coordinator.
            & $shellExecutable '-NoProfile' '-NonInteractive' '-File' (Join-Path $PSScriptRoot $scripts[$i])
            $scriptExitCode = $LASTEXITCODE
            if ($scriptExitCode -ne 0) {
                $statuses[$i] = "FEHLER (Exit-Code $scriptExitCode)"
                break
            }
            $statuses[$i] = 'OK'
        }
        finally {
            $durations[$i] = [long] [Math]::Floor($stopwatch.Elapsed.TotalSeconds) - $stepStarted
        }
    }
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    if ($scriptExitCode -eq 0 -or $scriptExitCode -eq 130) { $scriptExitCode = 1 }
}
finally {
    if ($null -ne $logFile -and $null -ne $stopwatch) {
        $stopwatch.Stop()
        Write-LogLine ''
        Write-LogLine 'Zeitprotokoll (HH:MM:SS)'
        Write-LogLine ("Start: {0}" -f $startedAt)
        Write-LogLine ("Ende:  {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'))
        for ($i = 0; $i -lt $scripts.Count; $i++) {
            if ($null -ne $durations[$i]) {
                Write-LogLine ('{0,-28} {1}  {2}' -f $scripts[$i], (Format-Duration $durations[$i]), $statuses[$i])
            }
            else {
                Write-LogLine ('{0,-28} --:--:--  Nicht ausgeführt' -f $scripts[$i])
            }
        }
        Write-LogLine ('{0,-28} {1}' -f 'Gesamt', (Format-Duration ([long] [Math]::Floor($stopwatch.Elapsed.TotalSeconds))))
        Write-LogLine ("Exit-Code: {0}" -f $scriptExitCode)
        Write-LogLine ("Logdatei: {0}" -f $logFile)
    }
}
exit $scriptExitCode
