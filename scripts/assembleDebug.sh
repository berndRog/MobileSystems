#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)

# Remove only each app's local configuration cache; Gradle recreates it.
for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue
   rm -rf -- "$dir/.gradle/configuration-cache"
done

for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue
   name=${dir%/}
   printf 'Build: %s\n' "${name##*/}"
   "$repo_root/gradlew" -p "$dir" assembleDebug --quiet --warning-mode=none
   printf 'OK: %s\n' "${name##*/}"
done
