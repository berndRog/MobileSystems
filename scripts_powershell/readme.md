# PowerShell scripts for MobileSystems

English | [Deutsch](readme_ger.md)

These scripts build and test the independent Android apps, create another app, or export the entire repository as ZIP. The repository root is not an aggregate Gradle build. Build/test scripts process app directories in sorted order; each app includes `Shared`.

## Requirements and invocation

- **PowerShell 7** (`pwsh`), not Windows PowerShell 5.1. ZIP export requires **7.4 or later** and Git in `PATH`.
- A complete checkout including `gradlew.bat`, `gradle/`, `build-logic/`, and `Shared/`.
- For builds/tests: a suitable JDK (the project uses a Java 21 toolchain), the Android SDK, and the platforms required by Gradle. The first run may download dependencies.
- Configure the SDK through `ANDROID_HOME` or suitable app-local `local.properties` files.
- Instrumented tests need a running emulator or an authorized Android device.
- Only app generation on Windows requires Developer Mode or permission to create symbolic links. Without these rights, generation fails and removes its temporary directory.
- The local execution policy must permit the reviewed scripts. No system-wide execution policy is changed.

Windows does not require Bash, WSL, Git Bash, or Unix tools. Build/test scripts use `gradlew.bat` on Windows and the existing executable Unix `gradlew` on macOS/Linux. See the [Bash documentation](../scripts/readme.md) for the shell variants.

Examples below run in PowerShell from the repository root:

~~~powershell
# Adjust the SDK path to your computer
$env:ANDROID_HOME = "C:\Users\Name\AppData\Local\Android\Sdk"
pwsh -NoProfile -File .\scripts_powershell\assembleDebug.ps1
~~~

All scripts determine the project root through `$PSScriptRoot` and work from other working directories. Quote paths with spaces:

~~~powershell
pwsh -NoProfile -File "C:\Android Projects\MobileSystems\scripts_powershell\runDebugChecks.ps1"
# From an existing PowerShell session:
& "C:\Android Projects\MobileSystems\scripts_powershell\create-app-project.ps1" A6_01_Sensors
$LASTEXITCODE
~~~

Execute scripts; do not dot-source them. On macOS/Linux, the five build/test/generator scripts can also run directly through their shebang and executable permissions when `pwsh` is in `PATH`:

~~~sh
./scripts_powershell/assembleDebug.ps1
./scripts_powershell/create-app-project.ps1 A6_01_Sensors
# An explicit PowerShell invocation also works:
pwsh -NoProfile -File ./scripts_powershell/exportZip.ps1
~~~

## What each script does

| Script | Purpose | Files changed or generated |
| --- | --- | --- |
| [assembleDebug.ps1](assembleDebug.ps1) | Builds the debug version of every app. | First removes all apps' local Gradle configuration caches; Gradle generates build outputs. |
| [testDebugUnitTest.ps1](testDebugUnitTest.ps1) | Runs local JVM tests for Shared once, then each app. | Gradle test outputs and reports. |
| [connectedDebugAndroidTest.ps1](connectedDebugAndroidTest.ps1) | Runs device/emulator tests for Shared once, then apps with Android tests. | Replaces the corresponding connected-test results and generates reports. |
| [runDebugChecks.ps1](runDebugChecks.ps1) | Runs build, local tests, and Android tests in that order. | Outputs of those steps and a timing log under `logs/`. |
| [create-app-project.ps1](create-app-project.ps1) | Creates a starter app using existing Gradle templates. | A new app directory; existing apps and Shared remain unchanged. |
| [exportZip.ps1](exportZip.ps1) | Exports the current saved project files as ZIP. | An archive; project source files remain unchanged. |

## Build and test

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\assembleDebug.ps1
pwsh -NoProfile -File .\scripts_powershell\testDebugUnitTest.ps1
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1

# All three phases, stopping at the first failure and writing a timing log:
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
$LASTEXITCODE
~~~

The three individual invocations are independent. Use `runDebugChecks.ps1` for a complete sequence that automatically stops at the first failure.

`assembleDebug.ps1` first removes only `.gradle/configuration-cache` from all matching apps, then invokes the wrapper with `-p <app> assembleDebug`. The global Gradle dependency cache remains intact.

`testDebugUnitTest.ps1` uses the first app's settings to run `:Shared:testDebugUnitTest` exactly once. It then runs `:testDebugUnitTest` for each app. The leading colon selects the app's task without rerunning Shared's tests.

`connectedDebugAndroidTest.ps1` follows the same pattern with `:Shared:connectedDebugAndroidTest` and `:connectedDebugAndroidTest`. Modules without Kotlin/Java files under `src/androidTest` are skipped. Old results under `build/outputs/androidTest-results/connected/debug` are removed before the module runs. After a successful Gradle invocation, at least one `TEST-*.xml` must exist directly in that directory; otherwise the script fails.

When multiple devices are connected, select one explicitly. This environment variable remains set in the current PowerShell session until removed and is inherited by child processes:

~~~powershell
$env:ANDROID_SERIAL = "emulator-5554"
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1
# Alternatively, run the complete sequence:
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
Remove-Item Env:ANDROID_SERIAL
~~~

Typical reports are `<app>/build/reports/tests/testDebugUnitTest/index.html` and `<app>/build/reports/androidTests/connected/debug/index.html`, with equivalent paths under `Shared/build/` for Shared.

Scripts stop at the first failure and use `--quiet --warning-mode=none`. For diagnosis, rerun an individual task with normal Gradle output:

~~~powershell
# Windows
.\gradlew.bat -p A3_05_SwipeDeleteUndo :testDebugUnitTest
# macOS/Linux in PowerShell
./gradlew -p A3_05_SwipeDeleteUndo :testDebugUnitTest
~~~

Do not run app builds or these scripts concurrently: Shared and build-logic use shared output directories.

### Timing log and exit codes

`runDebugChecks.ps1` launches the three steps in separate PowerShell processes and stops at the first failure. A `finally` block writes start/end times, durations, statuses, steps not executed, and the exit code to `logs/debug-checks-<timestamp>-<PID>.log` and the console. Times include a UTC offset.

The file contains phase-start messages and the summary, not the complete Gradle output. On normal Ctrl+C cancellation, the summary is written on a best-effort basis; forced process termination cannot be handled.

Native exit codes are explicitly checked and passed through. Success returns 0; script-generated errors normally return 1, and invalid generator arguments return 2. Check `$LASTEXITCODE` after an invocation.

## Create another app

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\create-app-project.ps1 A6_01_Sensors
~~~

Supply exactly one positional argument matching `A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*`. The directory and derived application ID must be unused. For `A6_01_Sensors`, the ID is `de.rogallab.mobile.a601sensors`.

The generator checks prerequisites and collisions, copies `settings.gradle.kts` and `build.gradle.kts` unchanged from `A2_02_Layout`, and creates a launcher manifest and a small Compose `MainActivity` displaying the app name. Generated text uses UTF-8 without BOM and LF; template files are copied byte-for-byte.

`gradle/`, `gradle.properties`, `gradlew`, and `gradlew.bat` are linked to the repository root; an existing root `local.properties` is linked too. Unlike the Bash generator, this variant uses **absolute symbolic-link targets** for compatibility with PowerShell 7.0. Update these links after moving the checkout. There is no silent fallback to independent file copies.

Generation starts in a temporary sibling directory. Failures and normal cancellation clean it up; an existing target is never overwritten. The app is not built or opened automatically. Open its directory directly in Android Studio and validate it:

~~~powershell
# Windows; use ./gradlew instead on macOS/Linux
.\gradlew.bat -p A6_01_Sensors projects
.\gradlew.bat -p A6_01_Sensors assembleDebug
.\gradlew.bat -p A6_01_Sensors :testDebugUnitTest
~~~

Commit the generated source/configuration files and Gradle links; keep `local.properties` local.

<a id="projekt-als-zip-exportieren"></a>

## Export the project as ZIP

[exportZip.ps1](exportZip.ps1) replaces Android Studio's “Export ZIP” for the entire project root, including all apps, `Shared`, `build-logic`, and Gradle wrappers under a parent folder `MobileSystems/`.

Requirements: **PowerShell 7.4 or later**, Git in `PATH`, and a Git working tree rooted at the script's project directory. Export does not require Java, the Android SDK, or an external ZIP tool.

~~~powershell
# Timestamped archive in the project root
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1
# Explicit target; its parent directory must exist
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1 -Destination "C:\Backups\MobileSystems course.zip"
# Explicit permission to replace an existing archive
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1 "C:\Backups\MobileSystems course.zip" -Force
~~~

~~~sh
# macOS, from any working directory
pwsh -NoProfile -File "/path with spaces/MobileSystems/scripts_powershell/exportZip.ps1" \
  -Destination "/tmp/MobileSystems course.zip"
~~~

The executable Bash entry point `scripts/exportZip.sh` calls the same implementation and also requires `pwsh` 7.4 or later and Git:

~~~sh
./scripts/exportZip.sh
./scripts/exportZip.sh "/tmp/MobileSystems course.zip"
./scripts/exportZip.sh --destination "/tmp/MobileSystems course.zip" --force
"/path with spaces/MobileSystems/scripts/exportZip.sh" "./My export.zip"
./scripts/exportZip.sh --help
~~~

The Bash entry point accepts `--force` (also `-Force`) and `--destination` (also `-Destination`). It passes through the exporter's exit code; invalid shell arguments return 2 and missing `pwsh` returns 1.

The project root is resolved from `$PSScriptRoot`. Relative destinations refer to the caller's working directory. Without a destination, the archive is `MobileSystems-yyyyMMdd-HHmmss-fff.zip` in the project root. Existing archives require `-Force`; there is no interactive confirmation. The exporter builds a temporary archive beside the destination and publishes it only after successful completion. Export errors go to stderr with exit code 1; success returns 0.

Selection uses current saved files, including modified and untracked files. Git is only the ignore engine (`git check-ignore --no-index -z --stdin`), not a source of committed or staged contents. Current ignore rules therefore apply to tracked files too. Git handles nested `.gitignore` files and `!` exceptions, including the rule that a file cannot be re-included while its parent directory remains excluded. `.git/info/exclude` and global Git exclusions also apply, so selection may differ between computers.

Additional exclusions override Git exceptions:

- Directories `.git`, `.gradle`, `.kotlin`, `.idea`, `build`, `out`, `log`/`logs`, `tmp`/`temp`, `.cache`/`caches`, `captures`, `.cxx`, and `.externalNativeBuild` at every level.
- `local.properties`, including links, `.DS_Store`, and `Thumbs.db`.
- Files ending in `.zip`, `.apk`, `.aab`, `.class`, `.dex`, `.hprof`, `.log`, `.tmp`, `.temp`, `.swp`, `.swo`, `.bak`, `.iml`, `.ipr`, `.iws`, or `~`. This excludes all previous ZIP exports regardless of their names.
- Existing Git rules additionally exclude items such as signing keys.

Specific exception: `gradle/wrapper/gradle-wrapper.jar` files, including app-local paths, remain included despite Git rules such as `*.jar`. Required shared Gradle files are checked before writing; missing or ignored required files cause an explanatory error.

Internal symbolic links are archived as normal files or directory contents. For directory links, Git checks the link paths and actual source paths with their respective ignore rules, because Git cannot directly check paths below a symbolic link. The extracted copy is independent of link permissions and old absolute targets. External, broken, or cyclic links fail the export.

Unix executable permissions are stored, but restoration depends on the extraction tool; `chmod +x gradlew` may be needed. Empty directories are omitted. An extracted archive has no Git metadata; exporting it again requires initializing its own Git working tree.

Avoid builds and file edits during export: it is not an atomic snapshot. Only saved files are read, not unsaved editor buffers. Reconfigure local SDK paths and ignored credentials after extraction. An archive-content check does not replace a Gradle build.

## Validation

The following records are retained from the existing documentation; this documentation transfer did not rerun builds or functional tests.

After renaming to `exportZip.ps1` and adding `scripts/exportZip.sh`, macOS tests with Bash 3.2 and PowerShell 7.6.9 produced archives with 1,767 entries and identical file contents through both entry points. Extraction and all 17 app directories were checked. Default destinations, foreign working directories, checkout/destination paths with spaces, Unicode destination names, overwrite protection, `--force`, invalid arguments, help, and missing `pwsh` were tested. All 3,554 checks passed, as did `bash -n` and `git diff --check`. No Windows execution was performed.

The original ZIP export was tested on macOS with PowerShell 7.6.9: an archive with 1,766 files was extracted into a temporary path with spaces. All 308 checks passed, covering 17 app structures, manifests, Gradle files, materialized wrappers, byte-identical contents including an already modified source file and the new untracked script, Shared, build-logic, and absence of excluded artifacts. Temporary-fixture checks covered nested ignore rules/exceptions, tracked ignored files, untracked Unicode/space-containing source names, wrapper JARs despite `*.jar`, default names, foreign working directories, overwrite protection, `-Force`, error codes, preservation of an old archive on failure, internal/external/broken/cyclic links, and macOS `/var`/`/private/var` aliases. No Gradle build of the extracted copy or Windows execution was performed.

The five original scripts were syntax-checked on macOS with PowerShell 7.4.6 through .NET, with 54 controlled functional tests in temporary projects with spaces in their paths. Gradle was replaced with a test double; only the coordinator's host lookup was mocked for the .NET-started test host. Checks covered execution order, Shared exactly once, error codes, missing/stale/nested reports, timing logs, generator arguments, application-ID collisions, and cleanup when link rights were unavailable. Generated manifest, activity, and both Gradle files were compared byte-for-byte with the Bash original. UTF-8 without BOM, LF, optional SDK configuration, and shared links were checked.

After adding OS-specific wrapper selection, all five scripts were syntax-checked again with PowerShell 7.6.9. A real macOS `assembleDebug.ps1` run successfully built all 17 apps.

**No Windows execution has been performed.** Windows link permissions, native batch invocations, Ctrl+C, filesystem behavior, and archive extraction still require validation there. macOS/Linux tests do not replace those checks. Before production use on Windows, check:

1. Invocation from a foreign working directory and a checkout with spaces.
2. Build/test order and propagation of Gradle error codes.
3. Stopping at the first failure, timing logs, and Ctrl+C.
4. Generator handling of valid/invalid names, existing targets, ID collisions, and missing link permissions.
5. Opening a generated app in Android Studio and building it with `gradlew.bat`.
