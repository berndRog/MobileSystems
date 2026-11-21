#!/usr/bin/env bash
# Shell entry point for the shared, cross-platform ZIP exporter.
# Requirements: Bash, PowerShell >= 7.4 (pwsh), Git.
set -euo pipefail

usage() {
    printf '%s\n' \
        'Aufruf: exportZip.sh [--force] [--destination ZIP | ZIP]' \
        'Ohne Ziel: MobileSystems-<Zeitstempel>.zip im Projektstamm.' \
        'Relative Ziele beziehen sich auf das aktuelle Arbeitsverzeichnis.' \
        '--force erlaubt ausdrücklich das Überschreiben eines bestehenden Archivs.' \
        'Benötigt PowerShell >= 7.4 (pwsh) und Git im PATH.'
}

fail() {
    printf 'Projekt-Export fehlgeschlagen: %s\n' "$1" >&2
    exit 2
}

destination=''
has_destination=false
force=false
while (($#)); do
    case "$1" in
        -h|--help)
            usage
            exit 0
            ;;
        --force|-Force)
            force=true
            shift
            ;;
        --destination|-Destination)
            (($# >= 2)) || fail "$1 benötigt einen ZIP-Dateipfad."
            [[ $has_destination == false ]] || fail 'Nur ein Zielpfad ist erlaubt.'
            [[ -n $2 ]] || fail 'Der Zielpfad darf nicht leer sein.'
            destination=$2
            has_destination=true
            shift 2
            ;;
        --)
            shift
            (($# == 1)) || fail 'Nach -- muss genau ein ZIP-Dateipfad stehen.'
            [[ $has_destination == false ]] || fail 'Nur ein Zielpfad ist erlaubt.'
            [[ -n $1 ]] || fail 'Der Zielpfad darf nicht leer sein.'
            destination=$1
            has_destination=true
            shift
            ;;
        -*)
            fail "Unbekannte Option: $1 (siehe --help)."
            ;;
        *)
            [[ $has_destination == false ]] || fail 'Nur ein Zielpfad ist erlaubt.'
            [[ -n $1 ]] || fail 'Der Zielpfad darf nicht leer sein.'
            destination=$1
            has_destination=true
            shift
            ;;
    esac
done

repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
exporter="$repo_root/scripts_powershell/exportZip.ps1"
[[ -f $exporter ]] || fail "PowerShell-Skript fehlt: $exporter"
if ! command -v pwsh >/dev/null 2>&1; then
    printf '%s\n' 'Projekt-Export fehlgeschlagen: PowerShell >= 7.4 fehlt. Bitte pwsh installieren und im PATH bereitstellen.' >&2
    exit 1
fi

arguments=()
if [[ $has_destination == true ]]; then
    arguments+=(-Destination "$destination")
fi
if [[ $force == true ]]; then
    arguments+=(-Force)
fi
# exec preserves the exporter's exit code and signal handling.
# Do not change cwd: relative destinations belong to the caller's directory.
exec pwsh -NoProfile -NonInteractive -File "$exporter" ${arguments[@]+"${arguments[@]}"}
