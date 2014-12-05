#! /bin/sh

set -e
cd ..

COMPONENTS="core ui-console rsp-hle audio-sdl video-rice"

for i in $COMPONENTS; do
    git rm -r "jni/mupen64plus-${i}"
    git commit --message="git: Remove ${i} in prep for subtree add --squash."
    git subtree add --squash --prefix="jni/mupen64plus-${i}" --message="${i}: Sync with upstream." https://github.com/mupen64plus-ae/mupen64plus-"${i}".git master
done

echo "Finished"
