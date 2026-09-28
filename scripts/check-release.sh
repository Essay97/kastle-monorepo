#!/bin/sh
# Run from the repository root. This check never publishes or changes Git state.
set -eu
fail() { echo "Release check failed: $*" >&2; exit 1; }
[ "$#" -eq 1 ] || fail 'usage: sh scripts/check-release.sh vMAJOR.MINOR.PATCH'
tag=$1
printf '%s\n' "$tag" | grep -Eq '^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$' || fail 'a stable version tag is required'
[ -z "$(git status --porcelain)" ] || fail 'working tree must be clean'
revision=$(git rev-parse --verify "refs/tags/${tag}^{commit}") || fail 'tag does not exist'
[ "$revision" = "$(git rev-parse HEAD)" ] || fail 'HEAD does not match the requested tag'
version=$(./gradlew -q appVersion)
[ "$tag" = "v$version" ] || fail "tag $tag does not match project version $version"
echo "Release verified: $tag ($revision)"
