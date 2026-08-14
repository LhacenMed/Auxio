#!/usr/bin/env bash
# lib/version.sh — version read/bump helpers for app/build.gradle.
# Source this file; do not execute directly.
#
# The version lives in a Groovy map literal rather than the flat versionName/versionCode
# pair Android projects usually carry, because a release has to know the difference between
# a stable build and an iteration of a pre-release:
#
#   ext.currentVersion = [
#       type        : "Stable",
#       versionMajor: 4,
#       versionMinor: 1,
#       versionPatch: 5,
#       versionBuild: 0,
#   ]

# ── Regex patterns (match the map literal in app/build.gradle) ─────────────────
_TYPE_RE='type[[:space:]]*:[[:space:]]*"[A-Za-z]+"'
_MAJOR_RE='versionMajor[[:space:]]*:[[:space:]]*([0-9]+)'
_MINOR_RE='versionMinor[[:space:]]*:[[:space:]]*([0-9]+)'
_PATCH_RE='versionPatch[[:space:]]*:[[:space:]]*([0-9]+)'
_BUILD_RE='versionBuild[[:space:]]*:[[:space:]]*([0-9]+)'

# version::read <gradle_file>
# Sets: V_TYPE, V_MAJOR, V_MINOR, V_PATCH, V_BUILD
version::read() {
    local file="$1"
    local block
    # grep -A is far more robust than an awk range pattern against CRLF/BOM
    # quirks that can otherwise hang or silently mismatch on Windows-edited files.
    block="$(grep -A 6 'ext.currentVersion' "$file")"

    if [[ -z "$block" ]]; then
        echo "✗ version::read — could not locate 'ext.currentVersion' block in ${file}" >&2
        exit 1
    fi

    V_TYPE="$(echo "$block" | grep -oE "$_TYPE_RE" | grep -oE '"[A-Za-z]+"' | tr -d '"')"
    V_MAJOR="$(echo "$block" | grep -oE "$_MAJOR_RE" | grep -oE '[0-9]+$')"
    V_MINOR="$(echo "$block" | grep -oE "$_MINOR_RE" | grep -oE '[0-9]+$')"
    V_PATCH="$(echo "$block" | grep -oE "$_PATCH_RE" | grep -oE '[0-9]+$')"
    V_BUILD="$(echo "$block" | grep -oE "$_BUILD_RE" | grep -oE '[0-9]+$')"
    V_BUILD="${V_BUILD:-0}"

    # Fail loudly instead of silently proceeding with empty version numbers.
    if [[ -z "$V_TYPE" || -z "$V_MAJOR" || -z "$V_MINOR" || -z "$V_PATCH" ]]; then
        echo "✗ version::read — failed to parse the version map from ${file}" >&2
        echo "  Parsed block was:" >&2
        echo "$block" >&2
        exit 1
    fi
}

# version::name <type> <major> <minor> <patch> [build]
# Prints the human-readable version string (mirrors toVersionName() in app/build.gradle).
version::name() {
    local type="$1" major="$2" minor="$3" patch="$4" build="${5:-0}"
    case "$type" in
        Alpha)            echo "${major}.${minor}.${patch}-alpha.${build}" ;;
        Beta)             echo "${major}.${minor}.${patch}-beta.${build}"  ;;
        ReleaseCandidate) echo "${major}.${minor}.${patch}-rc.${build}"    ;;
        *)                echo "${major}.${minor}.${patch}"                ;;
    esac
}

# version::code <major> <minor> <patch>
# Prints the versionCode (mirrors the formula in app/build.gradle).
version::code() {
    echo $(( $1 * 10000 + $2 * 100 + $3 ))
}

# version::bump <type> <bump_kind>
# bump_kind: major | minor | patch | build (build increments versionBuild for pre-release)
# Sets: V_MAJOR, V_MINOR, V_PATCH, V_BUILD (in place)
version::bump() {
    local type="$1" kind="$2"
    case "$kind" in
        major) V_MAJOR=$(( V_MAJOR + 1 )); V_MINOR=0; V_PATCH=0; V_BUILD=0 ;;
        minor) V_MINOR=$(( V_MINOR + 1 )); V_PATCH=0; V_BUILD=0             ;;
        patch) V_PATCH=$(( V_PATCH + 1 )); V_BUILD=0                        ;;
        build) V_BUILD=$(( V_BUILD + 1 ))                                   ;;
        *)     echo "✗ version::bump — unknown bump kind: ${kind}" >&2; exit 1 ;;
    esac
}

# version::write <gradle_file> <new_type> <major> <minor> <patch> [build]
# Rewrites the `ext.currentVersion` map in-place.
# Uses printf (not bash string \n interpolation) to build real newlines reliably,
# then passes the block to perl via an environment variable — never via shell
# string interpolation into the perl program text, which avoids any quoting/
# escaping hazard from the version values reaching the regex engine.
version::write() {
    local file="$1" type="$2" major="$3" minor="$4" patch="$5" build="${6:-0}"

    local new_block
    new_block="$(printf 'ext.currentVersion = [\n    type        : "%s",\n    versionMajor: %s,\n    versionMinor: %s,\n    versionPatch: %s,\n    versionBuild: %s,\n]' \
        "$type" "$major" "$minor" "$patch" "$build")"

    NEW_BLOCK="$new_block" perl -i -0777 -pe \
        's/ext\.currentVersion = \[.*?\n\]/$ENV{NEW_BLOCK}/s' \
        "$file" \
        || { echo "✗ version::write — perl substitution failed on ${file}" >&2; exit 1; }
}
