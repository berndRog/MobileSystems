#!/usr/bin/env pwsh
#requires -Version 7.0
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptExitCode = 0
$staging = $null
try {
    if ($args.Count -ne 1) {
        [Console]::Error.WriteLine('Aufruf: create-app-project.ps1 A4_02_CameraXPermissions')
        exit 2
    }
    $projectName = [string] $args[0]
    if ($projectName -cnotmatch '^A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*$') {
        [Console]::Error.WriteLine("Ungültiger Name: {0}", $projectName)
        [Console]::Error.WriteLine('Erwartet wird z. B. A6_01_Sensors.')
        exit 2
    }

    $repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).ProviderPath
    $target = Join-Path $repoRoot $projectName
    $template = Join-Path $repoRoot 'A2_02_Layout'
    # Get-Item also detects a dangling symbolic link at the target path.
    if ($null -ne (Get-Item -LiteralPath $target -Force -ErrorAction SilentlyContinue)) {
        throw "Der Projektordner existiert bereits: $target"
    }

    $requiredPaths = @(
        (Join-Path $template 'settings.gradle.kts'),
        (Join-Path $template 'build.gradle.kts'),
        (Join-Path $repoRoot 'Shared/build.gradle.kts'),
        (Join-Path $repoRoot 'build-logic/settings.gradle.kts'),
        (Join-Path $repoRoot 'build-logic/build.gradle.kts'),
        (Join-Path $repoRoot 'build-logic/src/main/kotlin/mobile.android.application.gradle.kts'),
        (Join-Path $repoRoot 'build-logic/src/main/kotlin/mobile.android.library.gradle.kts'),
        (Join-Path $repoRoot 'gradle/libs.versions.toml'),
        (Join-Path $repoRoot 'gradle.properties'),
        (Join-Path $repoRoot 'gradlew'),
        (Join-Path $repoRoot 'gradlew.bat')
    )
    foreach ($required in $requiredPaths) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
            throw "Benötigte Datei fehlt: $required"
        }
    }

    $applicationId = 'de.rogallab.mobile.' + [regex]::Replace($projectName.ToLowerInvariant(), '[^a-z0-9]', '')
    $projects = @(Get-ChildItem -LiteralPath $repoRoot -Directory |
        Where-Object { $_.Name -cmatch '^A[0-9].*_.*$' } |
        Sort-Object -Property Name -CaseSensitive)
    foreach ($project in $projects) {
        $existingId = 'de.rogallab.mobile.' + [regex]::Replace($project.Name.ToLowerInvariant(), '[^a-z0-9]', '')
        if ($applicationId -ceq $existingId) {
            throw "Die Application-ID ist bereits belegt durch $($project.Name): $applicationId"
        }
    }

    $staging = Join-Path $repoRoot ('.new-app.' + [Guid]::NewGuid().ToString('N'))
    [void] (New-Item -ItemType Directory -Path $staging)
    Copy-Item -LiteralPath (Join-Path $template 'settings.gradle.kts') -Destination (Join-Path $staging 'settings.gradle.kts')
    Copy-Item -LiteralPath (Join-Path $template 'build.gradle.kts') -Destination (Join-Path $staging 'build.gradle.kts')

    $linkNames = @('gradle', 'gradle.properties', 'gradlew', 'gradlew.bat')
    if (Test-Path -LiteralPath (Join-Path $repoRoot 'local.properties') -PathType Leaf) {
        $linkNames += 'local.properties'
    }
    foreach ($linkName in $linkNames) {
        try {
            # Absolute targets also support directory links in PowerShell 7.0.
            [void] (New-Item -ItemType SymbolicLink -Path (Join-Path $staging $linkName) -Target (Join-Path $repoRoot $linkName))
        }
        catch {
            throw "Verknüpfung '$linkName' konnte nicht erstellt werden. Unter Windows Entwicklermodus oder Rechte für symbolische Links prüfen. $($_.Exception.Message)"
        }
    }

    $sourceDirectory = Join-Path $staging 'src/main/java/de/rogallab/mobile/ui'
    [void] [IO.Directory]::CreateDirectory($sourceDirectory)
    $manifest = @'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
   <application
      android:label="__PROJECT_NAME__"
      android:theme="@android:style/Theme.Material.Light.NoActionBar">
      <activity
         android:name=".ui.MainActivity"
         android:exported="true">
         <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
         </intent-filter>
      </activity>
   </application>
</manifest>
'@
    $activity = @'
package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         MaterialTheme {
            Text("__PROJECT_NAME__")
         }
      }
   }
}
'@

    function Write-GeneratedFile {
        param([string] $Path, [string] $Content)
        $lf = [string] [char] 10
        $crlf = [string] [char] 13 + [char] 10
        $contentWithLf = $Content.Replace($crlf, $lf).TrimEnd([char[]] @([char] 13, [char] 10)) + $lf
        [IO.File]::WriteAllText($Path, $contentWithLf.Replace('__PROJECT_NAME__', $projectName), [Text.UTF8Encoding]::new($false))
    }

    Write-GeneratedFile -Path (Join-Path $staging 'src/main/AndroidManifest.xml') -Content $manifest
    Write-GeneratedFile -Path (Join-Path $sourceDirectory 'MainActivity.kt') -Content $activity

    # Unlike Move-Item, this cannot nest the staging folder inside a racing target.
    [IO.Directory]::Move($staging, $target)
    $staging = $null
    [Console]::WriteLine("Projekt erstellt: {0}", $target)
    [Console]::WriteLine("Application-ID: {0}", $applicationId)
    [Console]::WriteLine('In Android Studio diesen Projektordner direkt öffnen.')
}
catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    $scriptExitCode = 1
}
finally {
    if ($null -ne $staging -and (Test-Path -LiteralPath $staging -PathType Container)) {
        try {
            # Unlink top-level shared targets before recursively deleting staging.
            foreach ($entry in (Get-ChildItem -LiteralPath $staging -Force)) {
                if (($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                    Remove-Item -LiteralPath $entry.FullName -Force
                }
            }
            Remove-Item -LiteralPath $staging -Recurse -Force
        }
        catch {
            [Console]::Error.WriteLine("Temporärer Ordner konnte nicht entfernt werden: $staging. $($_.Exception.Message)")
            $scriptExitCode = 1
        }
    }
}
exit $scriptExitCode
