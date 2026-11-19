#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
repo_root=$(cd -- "$script_dir/.." && pwd -P)

scripts=(assembleDebug.sh testDebugUnitTest.sh connectedDebugAndroidTest.sh)
durations=()
statuses=()

# Check prerequisites before starting any build.
for script in "${scripts[@]}"; do
   if [[ ! -r "$script_dir/$script" ]]; then
      printf 'Skript fehlt oder ist nicht lesbar: %s\n' "$script_dir/$script" >&2
      exit 1
   fi
done

log_dir="$repo_root/logs"
mkdir -p -- "$log_dir"
log_file="$log_dir/debug-checks-$(date '+%Y%m%d-%H%M%S')-$$.log"
started_at=$(date '+%Y-%m-%d %H:%M:%S %Z')
SECONDS=0

format_duration() {
   local seconds=$1
   printf '%02d:%02d:%02d' "$((seconds / 3600))" "$((seconds / 60 % 60))" "$((seconds % 60))"
}

summarize() {
   local exit_code=$? total_seconds=$SECONDS i
   trap - EXIT
   {
      printf '\nZeitprotokoll (HH:MM:SS)\n'
      printf 'Start: %s\n' "$started_at"
      printf 'Ende:  %s\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')"
      for i in "${!scripts[@]}"; do
         if [[ -n ${durations[$i]:-} ]]; then
            printf '%-28s %s  %s\n' "${scripts[$i]}" "$(format_duration "${durations[$i]}")" "${statuses[$i]}"
         else
            printf '%-28s --:--:--  Nicht ausgeführt\n' "${scripts[$i]}"
         fi
      done
      printf '%-28s %s\n' 'Gesamt' "$(format_duration "$total_seconds")"
      printf 'Exit-Code: %s\n' "$exit_code"
      printf 'Logdatei: %s\n' "$log_file"
   } | tee -a "$log_file"
   exit "$exit_code"
}
trap summarize EXIT

for i in "${!scripts[@]}"; do
   printf '\nStarte: %s\n' "${scripts[$i]}" | tee -a "$log_file"
   step_started=$SECONDS
   exit_code=0
   bash "$script_dir/${scripts[$i]}" || exit_code=$?
   durations[$i]=$((SECONDS - step_started))
   if ((exit_code != 0)); then
      statuses[$i]="FEHLER (Exit-Code $exit_code)"
      exit "$exit_code"
   fi
   statuses[$i]='OK'
done
