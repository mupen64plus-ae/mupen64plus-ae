#! /bin/sh

set -e

# This is more an example how to merge upstream sources directly than an
# actual script because it breaks quite easily when detecting a merge problem.

# From https://github.com/git/git/blob/master/contrib/subtree/git-subtree.sh
curl https://raw.github.com/git/git/master/contrib/subtree/git-subtree.sh > git-subtree.sh
cd ..
GITSUBTREE=./tools/git-subtree.sh

sh ${GITSUBTREE} pull --prefix=jni/audio-sdl https://github.com/mupen64plus/mupen64plus-audio-sdl.git master
sh ${GITSUBTREE} pull --prefix=jni/core https://github.com/mupen64plus/mupen64plus-core.git master
sh ${GITSUBTREE} pull --prefix=jni/rsp-hle https://github.com/mupen64plus/mupen64plus-rsp-hle.git master
sh ${GITSUBTREE} pull --prefix=jni/front-end https://github.com/mupen64plus/mupen64plus-ui-console.git master
sh ${GITSUBTREE} pull --prefix=jni/gles2glide64 https://github.com/mupen64plus/mupen64plus-video-glide64mk2.git master
sh ${GITSUBTREE} pull --prefix=jni/gles2rice https://github.com/mupen64plus/mupen64plus-video-rice.git master

echo "Finished"
