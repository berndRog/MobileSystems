#!/usr/bin/env bash
set -euo pipefail

usage() {
   printf 'Aufruf: %s A6_01_Sensors\n' "${0##*/}" >&2
   exit 2
}

[[ $# -eq 1 ]] || usage
name=$1
if [[ ! $name =~ ^A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*$ ]]; then
   printf 'Ungültiger Name: %s\nErwartet wird z. B. A6_01_Sensors.\n' "$name" >&2
   exit 2
fi

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
target=$repo_root/$name
template=$repo_root/A2_02_Layout

if [[ -e $target || -L $target ]]; then
   printf 'Der Projektordner existiert bereits: %s\n' "$target" >&2
   exit 1
fi

for required in \
   "$template/settings.gradle.kts" \
   "$template/build.gradle.kts" \
   "$repo_root/Shared/build.gradle.kts" \
   "$repo_root/gradle/android-common.gradle" \
   "$repo_root/gradle/libs.versions.toml" \
   "$repo_root/gradle.properties" \
   "$repo_root/gradlew" \
   "$repo_root/gradlew.bat"; do
   if [[ ! -e $required ]]; then
      printf 'Benötigte Datei fehlt: %s\n' "$required" >&2
      exit 1
   fi
done

# The common build script derives applicationId by stripping separators.
application_id="de.rogallab.mobile.$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"
for existing in "$repo_root"/A[0-9]*_*/; do
   [[ -d $existing ]] || continue
   existing_name=${existing%/}
   existing_name=${existing_name##*/}
   existing_id="de.rogallab.mobile.$(printf '%s' "$existing_name" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"
   if [[ $application_id == "$existing_id" ]]; then
      printf 'Die Application-ID ist bereits belegt durch %s: %s\n' "$existing_name" "$application_id" >&2
      exit 1
   fi
done

staging=$(mktemp -d "$repo_root/.new-app.XXXXXXXX")
trap 'rm -rf -- "$staging"' EXIT

cp -- "$template/settings.gradle.kts" "$staging/settings.gradle.kts"
cp -- "$template/build.gradle.kts" "$staging/build.gradle.kts"
ln -s ../gradle "$staging/gradle"
ln -s ../gradle.properties "$staging/gradle.properties"
ln -s ../gradlew "$staging/gradlew"
ln -s ../gradlew.bat "$staging/gradlew.bat"
if [[ -f $repo_root/local.properties ]]; then
   # Local SDK settings stay outside Git, just as in the existing projects.
   ln -s ../local.properties "$staging/local.properties"
fi

source_dir="$staging/src/main/java/de/rogallab/mobile/ui"
mkdir -p -- "$source_dir"
cat > "$staging/src/main/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
   <application
      android:label="$name"
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
EOF

cat > "$source_dir/MainActivity.kt" <<EOF
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
            Text("$name")
         }
      }
   }
}
EOF

mv -- "$staging" "$target"
trap - EXIT
printf 'Projekt erstellt: %s\nApplication-ID: %s\n' "$target" "$application_id"
printf 'In Android Studio diesen Projektordner direkt öffnen.\n'
