#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)

shared_tested=false
for dir in "$repo_root"/A[0-9]*_*/; do
   [[ -d $dir ]] || continue

   # Shared is included by each app's settings, but needs testing only once.
   if [[ $shared_tested == false ]]; then
      printf 'Unit-Tests: Shared\n'
      "$repo_root/gradlew" -p "$dir" :Shared:testDebugUnitTest --quiet --warning-mode=none
      printf 'OK: Shared\n'
      shared_tested=true
   fi

   name=${dir%/}
   printf 'Unit-Tests: %s\n' "${name##*/}"
   # The leading colon selects only the app's task, not Shared's tests.
   "$repo_root/gradlew" -p "$dir" :testDebugUnitTest --quiet --warning-mode=none
   printf 'OK: %s\n' "${name##*/}"
done
