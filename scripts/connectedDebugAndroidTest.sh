#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)

run_android_tests() {
   local source_dir=$1 project_dir=$2 task=$3
   local name=${source_dir##*/} results_dir
   printf 'Android-Tests: %s\n' "$name"
   if [[ ! -d "$source_dir/src/androidTest" ]] ||
      [[ -z $(find "$source_dir/src/androidTest" -type f \( -name '*.kt' -o -name '*.java' \) -print -quit) ]]; then
      printf 'Übersprungen (keine Android-Tests): %s\n' "$name"
      return
   fi

   results_dir="$source_dir/build/outputs/androidTest-results/connected/debug"
   rm -rf -- "$results_dir"
   "$repo_root/gradlew" -p "$project_dir" "$task" --quiet --warning-mode=none
   if [[ -z $(find "$results_dir" -maxdepth 1 -type f -name 'TEST-*.xml' -print -quit 2>/dev/null) ]]; then
      printf 'Keine Testergebnisse für %s; Installation und Geräteverbindung prüfen.\n' "$name" >&2
      exit 1
   fi
   printf 'OK: %s\n' "$name"
}

shared_tested=false
for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue

   # Use the first app's settings to test the included Shared module once.
   if [[ $shared_tested == false ]]; then
      run_android_tests "$repo_root/Shared" "$dir" :Shared:connectedDebugAndroidTest
      shared_tested=true
   fi

   # The leading colon selects only the app's task, not Shared's tests.
   run_android_tests "${dir%/}" "$dir" :connectedDebugAndroidTest
done
