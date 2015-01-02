#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage:"
    echo "tools/auto-build.sh username@hostname.ext path/to/destination/folder"
    exit 1
fi

#TODO: Loop through all branches
currentBranch="master"

echo "Checking out branch '""$currentBranch""'"
cmd="git checkout ""$currentBranch"; $cmd
oldRevision=`git rev-parse --short HEAD`

echo "Executing git pull"
git pull
newRevision=`git rev-parse --short HEAD`

if [ "$oldRevision" == "$newRevision" ]; then
    echo "Nothing new to build"
    exit 0
else
    echo "Cleaning previous build"
    ant clean
    ndk-build clean
    echo "Building APK"
    ndk-build -j4
    ant debug
    if [ ! -f "bin/Mupen64Plus-debug.apk" ]; then
        echo "Error: Build failed"
        exit 1
    else
        #TODO: Sanitize branch name for use in filename
        sanitizedBranchName="$currentBranch"

        echo "Uploading APK to host"
        cmd="scp bin/Mupen64Plus-debug.apk ""$1"":""$2""/Mupen64PlusAE_""$sanitizedBranchName""_""$(date +'%Y%m%d%H%M')""_""$newRevision"".apk"; $cmd
        echo "Done"
        exit 0
    fi
fi

