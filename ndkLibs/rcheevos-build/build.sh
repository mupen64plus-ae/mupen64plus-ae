#!/usr/bin/env bash
#
# Regenerates the checked-in librcheevos.a prebuilts.
#
# Run this after updating the vendored source in ndkLibs/rcheevos/, then commit
# the resulting archives under ndkLibs/libs/<variant>/<abi>/.
#
# Usage:  ./build.sh [path-to-ndk]
#
# The NDK defaults to $ANDROID_NDK_HOME. Use the version pinned in
# build_common/version_common.gradle (ndkVersion) so the prebuilts match the
# toolchain the rest of the project is built with.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NDKLIBS="$(cd "$HERE/.." && pwd)"

NDK="${1:-${ANDROID_NDK_HOME:-}}"
if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "error: NDK not found. Pass it as an argument or set ANDROID_NDK_HOME." >&2
    echo "       Expected version: $(grep -o 'ndkVersion=\"[^\"]*\"' \
        "$NDKLIBS/../build_common/version_common.gradle" 2>/dev/null || echo 'see version_common.gradle')" >&2
    exit 1
fi

NDK_BUILD="$NDK/ndk-build"
[ -x "$NDK_BUILD" ] || NDK_BUILD="$NDK/ndk-build.cmd"

for VARIANT in release debug; do
    if [ "$VARIANT" = "debug" ]; then NDK_DEBUG=1; else NDK_DEBUG=0; fi

    OUT="$HERE/.build/$VARIANT"
    rm -rf "$OUT"
    mkdir -p "$OUT"

    echo "==> Building librcheevos.a ($VARIANT)"
    "$NDK_BUILD" \
        NDK_PROJECT_PATH=null \
        APP_BUILD_SCRIPT="$HERE/Android.mk" \
        NDK_APPLICATION_MK="$HERE/Application.mk" \
        NDK_OUT="$OUT/obj" \
        NDK_LIBS_OUT="$OUT/libs" \
        NDK_DEBUG=$NDK_DEBUG

    for ABI_DIR in "$OUT"/obj/local/*/; do
        ABI="$(basename "$ABI_DIR")"
        SRC="$ABI_DIR/librcheevos.a"
        DEST="$NDKLIBS/libs/$VARIANT/$ABI"
        if [ -f "$SRC" ]; then
            mkdir -p "$DEST"
            cp -f "$SRC" "$DEST/librcheevos.a"
            echo "    $VARIANT/$ABI  ->  $(du -h "$DEST/librcheevos.a" | cut -f1)"
        else
            echo "    warning: no archive produced for $ABI" >&2
        fi
    done

    rm -rf "$OUT"
done

echo
echo "Done. Commit the updated ndkLibs/libs/*/*/librcheevos.a files."
