#! /bin/sh

# We are now employing a git-submodule-like policy for synchronizing the
# upstream components.  That is, all changes to the upstream modules in
# the mupen64plus-ae repository must be made on the upstream side first,
# then pulled downstream using this script.  Any manual modifications made
# from the downstream side will be overwritten the next time this script
# is run.  That is very much intentional, as it enforces the top-down
# philosophy we are now adopting.
# 
# Developers may still manually modify the upstream modules from the down-
# stream side, as long as they do it in a branch that never gets merged
# back to master.  That is also very much intentional; it should still be
# easy for devs to experiment with upstream code while working in the
# downstream project.
# 
# Note that we are not actually using git-submodule, due to its infamous
# fragility.  We do not want to require our contributors to be git super-
# users.
# 
# The approach used here is much more robust than git-submodule, since the
# upstream code is always retained in the downstream repository's history.
# The approach used here simply emulates what a git superuser would do
# using only basic git and shell operations.  That means that any mistakes
# created by this commit can be fixed using basic git and shell commands.
# No black magic or human sacrifices are needed in a worst-case scenario.

set -e
cd ..
BASE_DIR=`pwd`

COMPONENTS_ALL="audio-sdl core rsp-hle ui-console video-glide64mk2 video-rice"
DEFAULT_ORG="mupen64plus"
DEFAULT_BRANCH="master"
DEFAULT_REV="HEAD"

echo
echo "Type the names of the upstream repositories you wish to pull, separated by whitespace."
echo "Choices: ${COMPONENTS_ALL}"
read COMPONENTS

echo
echo "Type the name of the upstream organization/user you wish to pull from."
echo "[${DEFAULT_ORG}]"
read ORG
if [ "$ORG" == "" ];then
    ORG=$DEFAULT_ORG
fi

echo
echo "Type the name of the upstream branch you wish you pull from."
echo "[${DEFAULT_BRANCH}]"
read BRANCH
if [ "$BRANCH" == "" ];then
    BRANCH=$DEFAULT_BRANCH
fi

echo
echo "Type the name of the upstream revision you wish you pull from."
echo "[${DEFAULT_REV}]"
read REV
if [ "$REV" == "" ];then
    REV=$DEFAULT_REV
fi

for i in $COMPONENTS; do
    DEST_DIR="jni/mupen64plus-${i}"
    CLONE_DIR="tmp/mupen64plus-${i}"
    BASE_URL="https://github.com/${ORG}/mupen64plus-${i}"
    CLONE_URL="${BASE_URL}.git"
    COMMIT_URL="${BASE_URL}/commit/"
    MSG_PREFIX="${i}: Update to commit "
    
    DIFF=`git diff ${DEST_DIR}`
    if [ "$DIFF" != "" ];then
        echo
        echo "Working directory is not clean.  Please commit, stash, or reset your changes."
        echo "Aborted"
        exit
    fi
    
    OLD_SYNC_HASH=`git log -1 --grep="${MSG_PREFIX}[0-9a-fA-F]\{7\}" --pretty=format:"%s" | sed -e 's/.*\([0-9a-fA-F]\{7\}\).*/\1/'`
    if [ "$OLD_SYNC_HASH" == "" ];then
        echo
        echo "This script has not yet been used to pull from ${BASE_URL}."
        echo "Please enter the hash of the last upstream commit that was pulled."
        echo "If you do not know, simply enter 'HEAD' (without quotes)."
        echo "This information is only used to generate an informative commit message."
        read OLD_SYNC_HASH
    fi
    echo
    echo "Updating from ${OLD_SYNC_HASH}..."
    
    rm -r -f "${CLONE_DIR}"
    echo
    echo "Cloning ${CLONE_URL}"
    git clone --branch $BRANCH --single-branch "${CLONE_URL}" "${CLONE_DIR}"
    
    cd "${CLONE_DIR}"
    git reset --hard $REV
    NEW_SYNC_HASH=`git log -1 --pretty=format:"%h"`
    NEW_MSG_SUBJECT="${MSG_PREFIX}${NEW_SYNC_HASH}."
    NEW_MSG_LINK="${COMMIT_URL}${NEW_SYNC_HASH}"
    NEW_MSG_BODY=`git log ${OLD_SYNC_HASH}..${NEW_SYNC_HASH} --pretty=format:"%h %s" --graph`
    echo
    echo "...to commit ${NEW_SYNC_HASH}"
    echo 
    echo "${NEW_MSG_SUBJECT}"
    echo
    echo "${NEW_MSG_LINK}"
    echo
    echo "${NEW_MSG_BODY}"
    
    cd "${BASE_DIR}"
    rm -r -f "${CLONE_DIR}/.git"
    rm -r -f "${DEST_DIR}"
    mv "${CLONE_DIR}" "${DEST_DIR}"
    git add --all "${DEST_DIR}/."
    DIFF=`git diff ${DEST_DIR}`
    
    DO_COMMIT="y"
    if [ "$NEW_MSG_BODY" == "" -a "$DIFF" == "" ];then
        echo
        echo "No upstream changes to pull.  Continue? [N/y]"
        read DO_COMMIT
    fi
    
    if [ "$DO_COMMIT" == "y" -o "$DO_COMMIT" == "Y" ];then
        git commit "${DEST_DIR}/." --allow-empty --message="${NEW_MSG_SUBJECT}" --message="" --message="${NEW_MSG_LINK}" --message="" --message="${NEW_MSG_BODY}"
    fi
done

echo "Finished"
