#! /bin/sh

set -e

M64P_COMPONENTS="core ui-console rsp-hle audio-sdl video-rice video-glide64mk2"

# This is more an example how to merge upstream sources directly than an
# actual script because it breaks quite easily when detecting a merge problem.

# From https://github.com/git/git/blob/master/contrib/subtree/git-subtree.sh
curl https://raw.github.com/git/git/master/contrib/subtree/git-subtree.sh > git-subtree.sh
cd ..
GITSUBTREE=./tools/git-subtree.sh

for i in $M64P_COMPONENTS; do

    case "$i" in
    "ui-console")
        ae_module="front-end"
        ;;
    "video-rice")
        ae_module="gles2rice"
        ;;
    "video-glide64mk2")
        ae_module="gles2glide64"
        ;;
    *)
        ae_module="${i}"
        ;;
    esac

    sh ${GITSUBTREE} pull --prefix="jni/${ae_module}" https://github.com/mupen64plus/mupen64plus-"${i}".git master
done

echo "Finished"
