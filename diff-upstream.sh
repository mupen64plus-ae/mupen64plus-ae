#! /bin/sh

mkdir -p tmp/upstream
cd tmp/upstream/

if [ -d "mupen64plus-core" ]; then
    echo "skipping hg clone"
else
    hg clone https://bitbucket.org/richard42/mupen64plus-core
fi

if [ -d "mupen64plus-ui-console" ]; then
    echo "skipping hg clone"
else
    hg clone https://bitbucket.org/richard42/mupen64plus-ui-console
fi

if [ -d "mupen64plus-rsp-hle" ]; then
    echo "skipping hg clone"
else
    hg clone https://bitbucket.org/richard42/mupen64plus-rsp-hle
fi

if [ -d "mupen64plus-audio-sdl" ]; then
    echo "skipping hg clone"
else
    hg clone https://bitbucket.org/richard42/mupen64plus-audio-sdl
fi

if [ -d "mupen64plus-video-rice" ]; then
    echo "skipping hg clone"
else
    hg clone https://bitbucket.org/richard42/mupen64plus-video-rice
fi

cd ../../

echo
echo
echo DIFF: mupen64plus-core
diff -ruN ./tmp/upstream/mupen64plus-core/src/   ./jni/core/src/   | diffstat -C

echo
echo
echo DIFF: mupen64plus-ui-console
diff -ruN ./tmp/upstream/mupen64plus-ui-console/src/   ./jni/front-end/src/   | diffstat -C

echo
echo
echo DIFF: mupen64plus-rsp-hle
diff -ruN ./tmp/upstream/mupen64plus-rsp-hle/src/   ./jni/rsp-hle/src/   | diffstat -C

echo
echo
echo DIFF: mupen64plus-audio-sdl
diff -ruN ./tmp/upstream/mupen64plus-audio-sdl/src/   ./jni/audio-sdl/src/   | diffstat -C

echo
echo
echo DIFF: mupen64plus-video-rice
diff -ruN ./tmp/upstream/mupen64plus-video-rice/src/   ./jni/gles2rice/src/   | diffstat -C
