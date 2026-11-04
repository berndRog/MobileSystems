#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)

for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue
   name=${dir%/}
   printf 'Android-Tests: %s\n' "${name##*/}"
   if [[ ! -d "$dir/src/androidTest" ]] ||
      [[ -z $(find "$dir/src/androidTest" -type f \( -name '*.kt' -o -name '*.java' \) -print -quit) ]]; then
      printf 'Übersprungen (keine Android-Tests): %s\n' "${name##*/}"
      continue
   fi

   results_dir="$dir/build/outputs/androidTest-results/connected/debug"
   rm -rf -- "$results_dir"
   "$repo_root/gradlew" -p "$dir" connectedDebugAndroidTest --quiet --warning-mode=none
   if [[ -z $(find "$results_dir" -maxdepth 1 -type f -name 'TEST-*.xml' -print -quit 2>/dev/null) ]]; then
      printf 'Keine Testergebnisse für %s; Installation und Geräteverbindung prüfen.\n' "${name##*/}" >&2
      exit 1
   fi
   printf 'OK: %s\n' "${name##*/}"
done
