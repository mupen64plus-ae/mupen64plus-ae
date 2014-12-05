#! /bin/sh

# This is more an example how to merge upstream sources directly than an
# actual script because it breaks quite easily when detecting a merge problem.
#
# If you don't have git subtree, get it here:
# From https://github.com/git/git/blob/master/contrib/subtree/git-subtree.sh
# curl https://raw.github.com/git/git/master/contrib/subtree/git-subtree.sh > git-subtree.sh
# GITSUBTREE=./tools/git-subtree.sh

set -e
cd ..

COMPONENTS_ALL="audio-sdl core rsp-hle ui-console video-glide64mk2 video-rice"

echo
echo "Type the names of the upstream repositories you wish to pull, separated by whitespace."
echo "Choices: ${COMPONENTS_ALL}"
echo
read COMPONENTS

for i in $COMPONENTS; do
    git subtree pull --squash --prefix="jni/mupen64plus-${i}" --message="${i}: Sync with upstream." https://github.com/mupen64plus-ae/mupen64plus-"${i}".git master
done

echo "Finished"
