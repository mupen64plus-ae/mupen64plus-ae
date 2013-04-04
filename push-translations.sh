#! /bin/sh 

# Push the transifex translation files using the command-line tool.

echo
echo "WARNING:"
echo
echo "This script pushes the translated files from your local filesystem to the remote"
echo "Transifex server. You should only use this after manually changing the local"
echo "translated files, and want to propagate those changes back to the translators."
echo
echo "Generally this is only used when fixing typography or formatting errors made by"
echo "translators, for example to:"
echo "  - change '...' (three periods) to the ellipses character"
echo "  - fix broken string formatting argument like '%1$s'"
echo "  - insert whitespace, newlines, etc."
echo
echo "Are you sure you want to push translation files (type YES to confirm)?"

read response
if [ "$response" = "YES" ]; then
    echo
    echo "Pushing translations..."
    tx push -t --skip
else
    echo "Push canceled"
fi
