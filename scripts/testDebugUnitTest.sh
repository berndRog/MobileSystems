#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)

for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue
   name=${dir%/}
   printf 'Unit-Tests: %s\n' "${name##*/}"
   "$repo_root/gradlew" -p "$dir" testDebugUnitTest --quiet --warning-mode=none
   printf 'OK: %s\n' "${name##*/}"
done
