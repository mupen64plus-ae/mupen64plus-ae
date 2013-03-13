#! /bin/sh

# Pull the transifex translation files using the command-line tool.

pattern='./res/values-*/strings.xml ./doc/publish/listing-*.txt'

git pull
tx pull -a -f

environment=`uname -o`
if [ "$environment" = "Cygwin" -o "$environment" = "Msys" ]; then
    echo "Converting line endings to CRLF"
    unix2dos $pattern
else
    echo "Converting line endings to LF"
    dos2unix $pattern
fi

git add $pattern
git commit $pattern -m "res: Updated translations."
