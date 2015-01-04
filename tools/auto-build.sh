#!/bin/bash

if [ "$#" -eq 4 ] && [ "$4" == "-f" ]; then
    forceBuild=true
elif [ "$#" -ne 3 ]; then
    echo "Usage:"
    echo "tools/auto-build.sh username@hostname.ext path/to/destination/folder path/to/key [-f]"
    exit 1
else
    forceBuild=false
fi

#TODO: Loop through all branches
currentBranch="master"

echo "Checking out branch '""$currentBranch""'"
cmd="git checkout ""$currentBranch"; $cmd
oldRevision=`git rev-parse --short HEAD`

echo "Executing git pull"
git pull
newRevision=`git rev-parse --short HEAD`

if [ "$oldRevision" == "$newRevision" ] && [ "$forceBuild" == false ]; then
    echo "Nothing new to build"
    exit 0
else
    if [ "$forceBuild" == true ]; then
        "Forcing auto-build"
    fi
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
        cmd="scp -v -i ""$3"" bin/Mupen64Plus-debug.apk ""$1"":""$2""/Mupen64PlusAE_""$sanitizedBranchName""_""$(date +'%Y%m%d%H%M')""_""$newRevision"".apk"; $cmd
        echo "Done"
        exit 0
    fi
fi

