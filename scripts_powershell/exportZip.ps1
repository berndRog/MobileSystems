#!/usr/bin/env pwsh
#requires -Version 7.4
<#
.SYNOPSIS
Exports the current working tree, using Git's ignore engine.
.PARAMETER Destination
ZIP path, relative to the caller's current directory. Parent must exist.
.PARAMETER Force
Explicit permission to replace an existing archive after successful creation.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)][string]$Destination,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$tempArchive = $null
try {
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $root = $root.TrimEnd([IO.Path]::DirectorySeparatorChar)
    $comparison = if ($IsWindows) { [StringComparison]::OrdinalIgnoreCase } else { [StringComparison]::Ordinal }
    $git = (Get-Command git -CommandType Application -ErrorAction Stop).Source

    # Raw UTF-8/NUL streams preserve spaces, Unicode, tabs and newlines in names.
    function Invoke-Git {
        param([string[]]$Arguments, [string]$InputText = '', [int[]]$AllowedCodes = @(0))
        $info = [Diagnostics.ProcessStartInfo]::new()
        $info.FileName = $git
        $info.WorkingDirectory = $root
        $info.UseShellExecute = $false
        $info.RedirectStandardInput = $true
        $info.RedirectStandardOutput = $true
        $info.RedirectStandardError = $true
        $info.StandardInputEncoding = [Text.UTF8Encoding]::new($false)
        $info.StandardOutputEncoding = [Text.UTF8Encoding]::new($false)
        $info.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
        foreach ($key in @('GIT_DIR', 'GIT_WORK_TREE', 'GIT_INDEX_FILE', 'GIT_COMMON_DIR')) {
            [void]$info.Environment.Remove($key)
        }
        foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
        $process = [Diagnostics.Process]::new()
        $process.StartInfo = $info
        try {
            [void]$process.Start()
            $stdout = $process.StandardOutput.ReadToEndAsync()
            $stderr = $process.StandardError.ReadToEndAsync()
            $process.StandardInput.Write($InputText)
            $process.StandardInput.Close()
            $process.WaitForExit()
            $output = $stdout.GetAwaiter().GetResult()
            $errors = $stderr.GetAwaiter().GetResult()
            if ($process.ExitCode -notin $AllowedCodes) {
                throw "Git fehlgeschlagen (Code $($process.ExitCode)): $errors"
            }
            return $output
        }
        finally { $process.Dispose() }
    }

    $gitRoot = (Invoke-Git -Arguments @('rev-parse', '--show-toplevel')).TrimEnd("`r", "`n")
    $prefix = (Invoke-Git -Arguments @('rev-parse', '--show-prefix')).TrimEnd("`r", "`n")
    if ($prefix.Length -ne 0) {
        throw "Der Skript-Projektstamm muss der Stamm eines Git-Arbeitsverzeichnisses sein: $root"
    }
    # Git canonicalizes ancestor links too (e.g. /var -> /private/var on macOS).
    $root = [IO.Path]::GetFullPath($gitRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
    if ([string]::IsNullOrWhiteSpace($Destination)) {
        $Destination = Join-Path $root ("MobileSystems-{0}.zip" -f (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
    }
    $target = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Destination)
    if ([IO.Path]::GetExtension($target) -ine '.zip') { throw 'Der Zielpfad muss auf .zip enden.' }
    $parent = [IO.Path]::GetDirectoryName($target)
    if (-not [IO.Directory]::Exists($parent)) { throw "Zielverzeichnis existiert nicht: $parent" }
    if ([IO.Directory]::Exists($target)) { throw "Ziel ist ein Verzeichnis: $target" }
    if ([IO.File]::Exists($target) -and -not $Force) {
        throw "Archiv existiert bereits: $target. Überschreiben nur mit -Force."
    }

    function Test-HardExcluded([string]$Path) {
        # These exclusions override ! exceptions, including for tracked files.
        return $Path -match '(?i)(^|/)(\.git|\.gradle|\.kotlin|\.idea|build|out|logs?|tmp|temp|\.cache|caches|captures|\.cxx|\.externalNativeBuild)(/|$)' -or
            $Path -match '(?i)(^|/)(local\.properties|\.DS_Store|Thumbs\.db)$' -or
            $Path -match '(?i)(\.zip|\.apk|\.aab|\.class|\.dex|\.hprof|\.log|\.tmp|\.temp|\.swp|\.swo|\.bak|\.iml|\.ipr|\.iws|~)$'
    }

    function Get-CanonicalPath([string]$Path) {
        $full = [IO.Path]::GetFullPath($Path)
        $parentPath = [IO.Path]::GetDirectoryName($full)
        if ([string]::IsNullOrEmpty($parentPath)) { return $full }
        $canonical = Join-Path (Get-CanonicalPath $parentPath) ([IO.Path]::GetFileName($full))
        $node = Get-Item -LiteralPath $canonical -Force
        if ($node.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            $targetNode = $node.ResolveLinkTarget($true)
            if ($null -eq $targetNode -or -not $targetNode.Exists) { throw "Defekter Link: $Path" }
            return Get-CanonicalPath $targetNode.FullName
        }
        return $canonical
    }

    $files = [Collections.Generic.List[object]]::new()
    function Add-Tree {
        param([string]$Physical, [string]$Relative, [string[]]$Ancestors, [string[]]$IgnoreLinks = @())
        if ($Relative -and (Test-HardExcluded $Relative)) { return }
        $item = Get-Item -LiteralPath $Physical -Force
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            $IgnoreLinks = $IgnoreLinks + @([IO.Path]::GetRelativePath($root, $Physical).Replace('\', '/'))
            $resolved = $item.ResolveLinkTarget($true)
            if ($null -eq $resolved -or -not $resolved.Exists) { throw "Defekter Link: $Relative" }
            $Physical = Get-CanonicalPath $resolved.FullName
            if (-not $Physical.StartsWith($root + [IO.Path]::DirectorySeparatorChar, $comparison)) {
                throw "Link zeigt außerhalb des Projekts: $Relative -> $Physical"
            }
            $item = Get-Item -LiteralPath $Physical -Force
        }
        $physicalRelative = [IO.Path]::GetRelativePath($root, $Physical).Replace('\', '/')
        if (Test-HardExcluded $physicalRelative) { return }
        if ($item.PSIsContainer) {
            foreach ($ancestor in $Ancestors) {
                if ($Physical.Equals($ancestor, $comparison)) { throw "Zyklischer Verzeichnislink: $Relative" }
            }
            foreach ($child in Get-ChildItem -LiteralPath $Physical -Force) {
                $name = if ($Relative) { "$Relative/$($child.Name)" } else { $child.Name }
                Add-Tree -Physical $child.FullName -Relative $name -Ancestors ($Ancestors + @($Physical)) -IgnoreLinks $IgnoreLinks
            }
        }
        else {
            $files.Add([pscustomobject]@{
                Relative = $Relative; Physical = $Physical
                IgnorePaths = @($IgnoreLinks) + @($physicalRelative)
            })
        }
    }
    Add-Tree -Physical $root -Relative '' -Ancestors @()

    # --no-index also applies ignore rules to files already tracked by Git.
    # Git handles nested .gitignore, negations, info/exclude and global excludes.
    $ignored = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    if ($files.Count -gt 0) {
        $inputNames = (($files.IgnorePaths | Sort-Object -Unique) -join "`0") + "`0"
        $result = Invoke-Git -Arguments @('check-ignore', '--no-index', '-z', '--stdin') -InputText $inputNames -AllowedCodes @(0, 1)
        foreach ($name in $result.Split([char]0, [StringSplitOptions]::RemoveEmptyEntries)) {
            [void]$ignored.Add($name)
        }
    }
    $selected = @($files | Where-Object {
        @($_.IgnorePaths | Where-Object { $ignored.Contains($_) }).Count -eq 0 -or
        $_.Relative -cmatch '(^|/)gradle/wrapper/gradle-wrapper\.jar$'
    } | Sort-Object Relative)
    $required = @('settings.gradle.kts', 'build.gradle.kts', 'gradle.properties',
        'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar',
        'gradle/wrapper/gradle-wrapper.properties', 'gradle/libs.versions.toml',
        'Shared/build.gradle.kts', 'build-logic/settings.gradle.kts')
    foreach ($name in $required) {
        if ($name -cnotin $selected.Relative) { throw "Notwendige Datei fehlt oder wird ignoriert: $name" }
    }

    # Build beside the destination; a failed export leaves an old ZIP intact.
    $tempArchive = Join-Path $parent ('.export-' + [Guid]::NewGuid().ToString('N') + '.tmp')
    $stream = [IO.File]::Open($tempArchive, [IO.FileMode]::CreateNew, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    try {
        $zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $true)
        try {
            foreach ($file in $selected) {
                $entry = [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                    $zip, $file.Physical, "MobileSystems/$($file.Relative)", [IO.Compression.CompressionLevel]::Optimal)
                $mode = 33188 # Unix regular file 0100644
                if ($IsWindows) {
                    if ($file.Relative -match '(^|/)gradlew$|\.sh$') { $mode = 33261 }
                }
                else {
                    $mode = 32768 -bor [int][IO.File]::GetUnixFileMode($file.Physical)
                }
                $entry.ExternalAttributes = $mode -shl 16
            }
        }
        finally { $zip.Dispose() }
    }
    finally { $stream.Dispose() }
    [IO.File]::Move($tempArchive, $target, [bool]$Force)
    $tempArchive = $null
    Write-Host "Export erstellt: $target ($($selected.Count) Dateien)"
    exit 0
}
catch {
    [Console]::Error.WriteLine("Projekt-Export fehlgeschlagen: $($_.Exception.Message)")
    exit 1
}
finally {
    if ($tempArchive -and [IO.File]::Exists($tempArchive)) {
        [IO.File]::Delete($tempArchive)
    }
}
