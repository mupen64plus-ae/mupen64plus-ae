#!/bin/bash

if [ "$#" -eq 4 ] && [ "$4" == "-f" ]; then
    # forceBuild will build all branches, whether or not there are changes
    forceBuild=true
elif [ "$#" -ne 3 ]; then
    echo "Usage:"
    echo "tools/auto-build.sh username@hostname.ext path/to/destination/folder path/to/key [-f]"
    exit 1
else
    forceBuild=false
fi

# Look up all local and remote branches for comparison
localBranches=($(git branch | awk -F ' +' '! /\(no branch\)/ {print $2}'))
remoteBranches=($(git branch -r | awk -F ' origin/+' '! /\->/ {print $2}'))

echo "Executing git fetch"
git fetch --prune --all

exitCode=0
# Loop through the remote branches (these are the only ones that may have changed)
for currentBranch in "${remoteBranches[@]}"; do
    # Determine if the remote branch is not among the local branches
    newBranch=true
    for b in "${localBranches[@]}"; do
        if [ "$b" == "$currentBranch" ]; then
            newBranch=false
        fi
    done
    
    echo "Checking out branch '""$currentBranch""'"
    cmd="git checkout ""$currentBranch"; $cmd
    if [ "$newBranch" == true ]; then
        echo "New branch"
        # Always build new branches ("(none)" won't match any revision number)
        oldRevision="(none)"
    else
        oldRevision=`git rev-parse --short HEAD`
    fi
    
    echo "Executing git reset"
    cmd="git reset --hard origin/""$currentBranch"; $cmd
    newRevision=`git rev-parse --short HEAD`
    
    # Compare local and remote revision numbers, and build if there are changes
    if [ "$oldRevision" == "$newRevision" ] && [ "$forceBuild" == false ]; then
        echo "Nothing new to build"
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
            # Exit with error code 1 when finished looping through the branches
            exitCode=1
        else
            # Sanatize the branch name for use as part of the APK filename
            sanitizedBranchName=${currentBranch//[^a-zA-Z0-9\.]/-}
            echo "Uploading APK to host"
            cmd="scp -v -i ""$3"" bin/Mupen64Plus-debug.apk ""$1"":""$2""/Mupen64PlusAE_""$sanitizedBranchName""_""$(date +'%Y%m%d%H%M')""_""$newRevision"".apk"; $cmd
        fi
    fi
done
# Make sure 'master' branch is checked out next time this script is executed from crontab
echo "Switching back to branch 'master'"
git checkout master
echo "Done"
exit $exitCode

