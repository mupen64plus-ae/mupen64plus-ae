#! /bin/sh

M64P_COMPONENTS="core ui-console rsp-hle audio-sdl video-rice video-glide64mk2"

for i in $M64P_COMPONENTS; do
    git remote|grep "^m64p-${i}$" > /dev/null
    if [ "$?" != "0" ]; then
        git remote add --no-tags "m64p-${i}" "https://github.com/mupen64plus/mupen64plus-${i}.git"
    fi
done

git remote update --prune

for i in $M64P_COMPONENTS; do
    echo
    echo
    echo "DIFF: mupen64plus-${i}"
    git --no-pager diff --color=auto --stat "m64p-${i}/master:src/" "HEAD:jni/mupen64plus-${i}/src/"
done
