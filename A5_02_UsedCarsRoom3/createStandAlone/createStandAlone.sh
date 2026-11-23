#!/usr/bin/env bash
# Eigenständige Kopie dieser App mit app/, Shared/, build-logic/ und gradle/.
# macOS / Linux: Bash >= 3.2 und rsync. Kein PowerShell erforderlich.
# Standardziel: A5_02_UseCarsRoom3 neben MobileSystems. Quellen bleiben unverändert.
set -euo pipefail

fail() {
    printf 'Standalone-Erstellung fehlgeschlagen: %s\n' "$1" >&2
    exit 1
}

usage() {
    printf '%s\n' \
        'Aufruf: bash createStandAlone/createStandAlone.sh [--destination ZIELORDNER]' \
        'Standard: ../A5_02_UseCarsRoom3 neben dem Ordner MobileSystems.' \
        'Ein alternatives Ziel muss ebenfalls A5_02_UseCarsRoom3 heißen.' \
        'Relative Ziele beziehen sich auf das aktuelle Arbeitsverzeichnis.' \
        'Bestehende Ziele werden niemals überschrieben.' \
        'Erstellt eine Projektkopie, kein ZIP. Danach den Zielordner in Android Studio öffnen.'
}

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
project_dir=$(cd -- "$script_dir/.." && pwd -P)
mobile_dir=$(cd -- "$project_dir/.." && pwd -P)
project_name=A5_02_UseCarsRoom3
destination="$(dirname -- "$mobile_dir")/$project_name"
case "${1:-}" in
    -h|--help) usage; exit 0 ;;
    --destination)
        (($# == 2)) || fail '--destination benötigt genau einen Zielordner.'
        [[ -n $2 ]] || fail 'Leerer Zielordner.'
        destination=$2
        ;;
    '') (($# == 0)) || fail 'Leeres Argument.' ;;
    *) fail 'Unbekanntes Argument (siehe --help).' ;;
esac

command -v rsync >/dev/null 2>&1 || fail 'rsync ist nicht installiert.'
[[ $(basename -- "$project_dir") == A5_02_UsedCarsRoom3 ]] ||
    fail 'Dieses Skript gehört zum Quellprojekt A5_02_UsedCarsRoom3.'
[[ $(basename -- "$destination") == "$project_name" ]] ||
    fail "Der Zielordner muss $project_name heißen."
parent=$(dirname -- "$destination")
[[ -d $parent ]] || fail "Ziel-Elternordner fehlt: $parent"
parent=$(cd -- "$parent" && pwd -P)
destination="$parent/$project_name"
[[ ! -e $destination && ! -L $destination ]] ||
    fail "Ziel existiert bereits: $destination"
case "$parent/" in
    "$mobile_dir/"*) fail 'Das Ziel muss außerhalb von MobileSystems liegen.' ;;
esac

for required in \
    "$project_dir/build.gradle.kts" \
    "$project_dir/settings.gradle.kts" \
    "$project_dir/gradle.properties" \
    "$project_dir/gradlew" \
    "$project_dir/gradlew.bat" \
    "$mobile_dir/Shared/build.gradle.kts" \
    "$mobile_dir/build-logic/settings.gradle.kts" \
    "$mobile_dir/build-logic/src/main/kotlin/mobile.android.application.gradle.kts" \
    "$mobile_dir/gradle/libs.versions.toml" \
    "$mobile_dir/gradle/wrapper/gradle-wrapper.jar" \
    "$mobile_dir/gradle/wrapper/gradle-wrapper.properties" \
    "$mobile_dir/proguard-rules.pro"; do
    [[ -f $required ]] || fail "Benötigte Datei fehlt: $required"
done
[[ -d "$project_dir/src" ]] || fail 'App-Quellcode fehlt.'

# Temporäres, eindeutig benanntes Geschwisterverzeichnis. Bei Fehlern aufräumen.
stage=$(mktemp -d "$parent/.createStandAlone.XXXXXX")
cleanup() { rm -rf -- "$stage"; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
work="$stage/$project_name"
mkdir -p -- "$work/app"

copy_options=(
    -aL
    --exclude=.git
    --exclude=.gradle
    --exclude=.kotlin
    --exclude=.idea
    --exclude=.cxx
    --exclude=.externalNativeBuild
    --exclude=build
    --exclude=out
    --exclude=captures
    --exclude=.cache
    --exclude=local.properties
    --exclude=.DS_Store
    --exclude=Thumbs.db
    --exclude=createStandAlone
    --exclude=zipScript
    '--exclude=*.zip'
    '--exclude=*.apk'
    '--exclude=*.aab'
    '--exclude=*.log'
    '--exclude=*.hprof'
    '--exclude=*.tmp'
    '--exclude=*.iml'
    '--exclude=*.bak'
    '--exclude=*.swp'
    '--exclude=*~'
)
# Nur App-Dateien ins Modul; Wrapper, Dokumentation und Einstellungen bleiben im Stamm.
rsync "${copy_options[@]}" \
    --exclude=/gradle --exclude=/gradlew --exclude=/gradlew.bat \
    --exclude=/settings.gradle.kts --exclude=/gradle.properties \
    '--exclude=/*.md' --exclude=/docs \
    "$project_dir/" "$work/app/"
rsync "${copy_options[@]}" "$mobile_dir/Shared/" "$work/Shared/"
rsync "${copy_options[@]}" "$mobile_dir/build-logic/" "$work/build-logic/"
# -L materialisiert Links; das Ergebnis benötigt keine Verknüpfungen zum Verbund.
rsync "${copy_options[@]}" "$mobile_dir/gradle/" "$work/gradle/"
for name in gradlew gradlew.bat gradle.properties settings.gradle.kts; do
    cp -p -- "$project_dir/$name" "$work/$name"
done
cp -p -- "$mobile_dir/proguard-rules.pro" "$work/proguard-rules.pro"
for document in "$project_dir/"*.md; do
    [[ -f $document ]] || continue
    cp -p -- "$document" "$work/"
done
if [[ -d "$project_dir/docs" ]]; then
    rsync "${copy_options[@]}" "$project_dir/docs/" "$work/docs/"
fi
chmod +x "$work/gradlew"

# Änderungen ausschließlich an den eben erstellten Kopien.
# Exakte Ersetzungen brechen bei unerwarteter Quellkonfiguration bewusst ab.
replace_once() {
    local file=$1 old=$2 replacement=$3 content remainder
    content=$(<"$file")
    [[ $content == *"$old"* ]] || fail "Erwartete Konfiguration fehlt in $file: $old"
    remainder=${content#*"$old"}
    [[ $remainder != *"$old"* ]] || fail "Konfiguration ist mehrdeutig in $file: $old"
    content=${content/"$old"/"$replacement"}
    printf '%s\n' "$content" > "$file"
}
replace_once "$work/settings.gradle.kts" \
    'includeBuild("../build-logic")' 'includeBuild("build-logic")'
replace_once "$work/settings.gradle.kts" \
    'rootProject.name = rootDir.name' 'rootProject.name = "A5_02_UseCarsRoom3"'
replace_once "$work/settings.gradle.kts" \
    'include(":Shared")' 'include(":app", ":Shared")'
replace_once "$work/settings.gradle.kts" \
    'project(":Shared").projectDir = file("../Shared")' \
    'project(":Shared").projectDir = file("Shared")'
replace_once "$work/settings.gradle.kts" \
    '// Only the selected application and the one shared library are imported.' \
    '// Standalone project: application and local shared library.'
replace_once "$work/app/build.gradle.kts" \
    '// Open this directory as a separate Gradle project; Shared is included by settings.gradle.kts.' \
    '// Application module of the standalone project.'
replace_once "$work/app/build.gradle.kts" \
    '// Shared Android library: ../Shared/build.gradle.kts' \
    '// Shared Android library in the sibling Shared module.'
replace_once "$work/build-logic/src/main/kotlin/mobile.android.application.gradle.kts" \
    'rootProject.file("../proguard-rules.pro")' 'rootProject.file("proguard-rules.pro")'

# Das Convention-Plugin leitet die Application-ID sonst vom neuen Modulnamen "app" ab.
printf '\n%s\n' \
    '// Preserve the application ID of the original A5_02_UsedCarsRoom3 project.' \
    'android {' \
    '    defaultConfig {' \
    '        applicationId = "de.rogallab.mobile.a502usedcarsroom3"' \
    '    }' \
    '}' >> "$work/app/build.gradle.kts"

printf '%s\n' \
    '// Root project. Android plugins are applied by app and Shared via build-logic.' \
    > "$work/build.gradle.kts"
printf '%s\n' \
    '.gradle/' '.kotlin/' '.idea/' '**/build/' 'local.properties' '*.iml' '.DS_Store' \
    > "$work/.gitignore"

# Ziel ausschließlich neu anlegen, dann den fertigen Inhalt übertragen.
# mkdir schützt auch vor einem inzwischen von anderer Seite angelegten Ziel.
mkdir -- "$destination" || fail "Ziel konnte nicht neu angelegt werden: $destination"
if ! rsync -a "$work/" "$destination/"; then
    fail "Kopie unvollständig; neu angelegten Zielordner prüfen: $destination"
fi
printf 'Standalone-Projekt erstellt: %s\n' "$destination"
printf '%s\n' 'Diesen Ordner in Android Studio öffnen und Gradle synchronisieren.' \
    'Das Android SDK wird dort lokal eingerichtet; local.properties wurde nicht kopiert.'
