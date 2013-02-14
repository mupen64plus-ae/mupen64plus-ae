# Pull the transifex translation files using the command-line tool.

git pull
tx pull -a -f
git add ./res/values*/strings.xml
git commit ./res/values*/strings.xml -m "res: Updated translations."
