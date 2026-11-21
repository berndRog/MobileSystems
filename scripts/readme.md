# Shell scripts for MobileSystems

English | [Deutsch](readme_ger.md)

These scripts build and test the independent Android apps, create another app, or export the entire repository as a ZIP. The repository root is not an aggregate Gradle build. The build and test scripts process app directories matching `A[0-9]*_*` one at a time; each app includes the shared library `Shared`.

## Requirements and invocation

- Use Bash, including macOS's Bash 3.2, and standard Unix command-line tools. Run the scripts directly or with `bash`, not `sh`. Do not source them.
- Builds and tests need the complete checkout, an executable root `gradlew`, a suitable JDK (the project uses a Java 21 toolchain), and the Android SDK/platforms required by the Gradle configuration. Gradle may download dependencies on the first run.
- Set `ANDROID_HOME` to the SDK location or provide suitable app-local `local.properties` files. The SDK configuration is machine-specific.
- Instrumented tests also need a running emulator or an authorized Android device.
- Only ZIP export additionally needs **PowerShell 7.4 or later** (`pwsh`) and Git in `PATH`.
- For Windows, see the [PowerShell scripts](../scripts_powershell/readme.md).

Examples below start in the repository root:

~~~bash
export ANDROID_HOME="$HOME/Library/Android/sdk" # macOS example; adjust as needed
./scripts/assembleDebug.sh
# Equivalent:
bash ./scripts/assembleDebug.sh
~~~

Each script determines the repository root from its own location and also works from a different working directory. Quote paths containing spaces:

~~~bash
"/path with spaces/MobileSystems/scripts/assembleDebug.sh"
~~~

## What each script does

| Script | Purpose | Files changed or generated |
| --- | --- | --- |
| [assembleDebug.sh](assembleDebug.sh) | Builds the debug version of every app. | Deletes each app's local Gradle configuration cache; Gradle creates build outputs. |
| [testDebugUnitTest.sh](testDebugUnitTest.sh) | Runs local JVM tests for Shared once, then each app. | Gradle test outputs and reports. |
| [connectedDebugAndroidTest.sh](connectedDebugAndroidTest.sh) | Runs device/emulator tests for Shared once, then each app that has Android tests. | Replaces the corresponding connected-test results and generates reports. |
| [runDebugChecks.sh](runDebugChecks.sh) | Runs build, local tests, and Android tests in that order. | Outputs from those steps plus a timing log in `logs/`. |
| [create-app-project.sh](create-app-project.sh) | Creates a new starter app from the existing Gradle templates. | A new app directory; existing apps and Shared remain unchanged. |
| [exportZip.sh](exportZip.sh) | Exports the current project files through the common PowerShell exporter. | A ZIP archive; project source files remain unchanged. |

## Build and test

~~~bash
./scripts/assembleDebug.sh
./scripts/testDebugUnitTest.sh
./scripts/connectedDebugAndroidTest.sh

# All three phases, with a timing log:
./scripts/runDebugChecks.sh

# Alternatively, explicitly chain the individual scripts:
./scripts/assembleDebug.sh &&
./scripts/testDebugUnitTest.sh &&
./scripts/connectedDebugAndroidTest.sh
~~~

`assembleDebug.sh` first removes `.gradle/configuration-cache` from all matching apps, then calls the root Gradle wrapper with `-p <app> assembleDebug`. It does not delete the global Gradle dependency cache.

`testDebugUnitTest.sh` uses the first app's Gradle settings to run `:Shared:testDebugUnitTest` exactly once. It then runs `:testDebugUnitTest` for each app. The leading colon selects the app's task without rerunning Shared's tests.

`connectedDebugAndroidTest.sh` follows the same Shared-once pattern with `:Shared:connectedDebugAndroidTest` and `:connectedDebugAndroidTest`. A module is skipped if `src/androidTest` contains no Kotlin or Java files. Before running a module, the script deletes its old `build/outputs/androidTest-results/connected/debug` directory. A successful Gradle exit alone is insufficient: the script also requires a `TEST-*.xml` directly in that directory. Missing results cause exit code 1.

If several devices are connected, select one explicitly:

~~~bash
ANDROID_SERIAL=emulator-5554 ./scripts/connectedDebugAndroidTest.sh
# The coordinator also passes this environment variable to its steps:
ANDROID_SERIAL=emulator-5554 ./scripts/runDebugChecks.sh
~~~

Typical reports are located at:

- `<app>/build/reports/tests/testDebugUnitTest/index.html`
- `<app>/build/reports/androidTests/connected/debug/index.html`
- The equivalent directories under `Shared/build/` for Shared's tests.

All build/test scripts stop at the first failure. They use `--quiet --warning-mode=none`. To diagnose a failure, rerun the affected task with normal Gradle output:

~~~bash
./gradlew -p A3_05_SwipeDeleteUndo :testDebugUnitTest
~~~

Do not run multiple app builds or these scripts concurrently: the apps share the output directories of Shared and build-logic.

### Timing log

`runDebugChecks.sh` starts the three scripts in separate Bash processes. It stops at the first failed phase and preserves its exit code. It prints a summary and writes `logs/debug-checks-<yyyyMMdd-HHmmss>-<PID>.log` with start/end times, durations, statuses, skipped phases, and the exit code.

The log contains phase-start messages and the timing summary, not the full Gradle output. An EXIT trap writes the summary on normal exit, including failures; forced termination such as SIGKILL cannot be handled.

## Create another app

~~~bash
./scripts/create-app-project.sh A6_01_Sensors
~~~

Supply exactly one name matching `A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*`. The directory must not exist, and the derived application ID must be unused. For `A6_01_Sensors`, the ID is `de.rogallab.mobile.a601sensors`.

The script:

1. Checks the name, template files, shared Gradle configuration, and application-ID collisions.
2. Copies `settings.gradle.kts` and `build.gradle.kts` from `A2_02_Layout`.
3. Creates a launcher manifest and a small Compose `MainActivity` displaying the app name.
4. Creates relative symbolic links to the root `gradle/`, `gradle.properties`, `gradlew`, and `gradlew.bat`. If a root `local.properties` exists, it links that too.
5. Moves the completed temporary directory to the requested app directory. On failure, its EXIT trap removes the temporary directory.

It does not build or open the app automatically. Open the new app directory directly in Android Studio and check it, for example:

~~~bash
./gradlew -p A6_01_Sensors projects
./gradlew -p A6_01_Sensors assembleDebug
./gradlew -p A6_01_Sensors :testDebugUnitTest
~~~

Commit the generated source/configuration files and shared Gradle links; keep `local.properties` local. Invalid argument counts/names return code 2; an existing target, missing prerequisites, or an ID collision returns a nonzero code.

## Export the project as ZIP

~~~bash
./scripts/exportZip.sh
./scripts/exportZip.sh "/tmp/MobileSystems course.zip"
./scripts/exportZip.sh --destination "/tmp/MobileSystems course.zip" --force
./scripts/exportZip.sh --help
~~~

This Bash entry point calls [exportZip.ps1](../scripts_powershell/exportZip.ps1). It requires `pwsh` and does not implement a separate ZIP algorithm.

- The archive contains a parent folder `MobileSystems/` with all included apps, Shared, build-logic, Gradle configuration, and wrappers.
- Without a target, the ZIP is created in the project root as `MobileSystems-yyyyMMdd-HHmmss-fff.zip`.
- A relative target is resolved against the caller's working directory. The parent directory must already exist.
- Existing archives are only replaced with explicit `--force` (also accepted as `-Force`). `--destination` also accepts the spelling `-Destination`.
- The exporter uses current saved files, including untracked files, and Git's ignore engine for nested `.gitignore` rules and exceptions. Global Git excludes and `.git/info/exclude` also apply.
- Additional exclusions remove build outputs, caches, logs, temporary files, local SDK settings, IDE metadata, `.git`, and all ZIP files. Wrapper JARs are retained despite rules such as `*.jar`.
- Internal symbolic links are materialized. External, broken, or cyclic links cause an error.
- The source must be a Git working tree rooted at the project directory. The export is not an atomic snapshot; avoid editing files or running builds while it runs.

See the [detailed export documentation and validation](../scripts_powershell/readme.md#projekt-als-zip-exportieren) for the full exclusions and limitations. After extraction, configure the local SDK and ignored credentials again; depending on the extraction tool, restore executable permission on `gradlew`.

Success returns 0. Export failures and missing `pwsh` return 1; invalid shell arguments return 2. The Bash entry point passes through the PowerShell exporter's exit code.
