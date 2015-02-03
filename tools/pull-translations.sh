#! /bin/sh

# Pull the transifex translation files using the command-line tool.

cd ..
pattern='./res/values-*/strings.xml ./doc/publish/listing-*.txt'

git pull
tx pull -a -f

git add $pattern
git commit $pattern -m "res: Updated translations."
