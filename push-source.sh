#! /bin/sh 

# Push the transifex source files using the command-line tool.

echo
echo "WARNING:"
echo
echo "This script pushes the source (English) files from your local filesystem to the"
echo "remote Transifex server.  If you use this script too often, you will fatigue the"
echo "translators.  Try not to use this script more than once per week."
echo
echo "Are you sure you want to push source files (type YES to confirm)?"

read response
if [ "$response" = "YES" ]; then
    echo
    echo "Pushing source..."
    tx push -s
else
    echo "Push canceled"
fi
