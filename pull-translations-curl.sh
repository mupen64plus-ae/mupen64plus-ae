# Pull the transifex translations using curl. Use this script only if you are unable to install the transifex command-line
# tool. IMPORTANT: This script must be revised whenever a language is added/removed from the list of available translations.

git pull

# Prompt for the username
echo "Transifex username:"
read username

# Remember session using cookie (doesn't seem to work; Transifex probably uses javascript-based login)
cookies="cookies.tmp"
curl -u "$username" -o "/dev/null" -c "$cookies" -k -s -L "https://www.transifex.com/signin/"

# Download the files by iterating through the language codes
# IMPORTANT: This list of languages must be kept up to date!
for lang in "de" "es" "fr" "gl" "hr" "it" "ja" "nl" "pl" "pt" "ru" "tr"
do
filename="res/values-$lang/strings.xml"
url="https://www.transifex.com/api/2/project/mupen64plus-ae/resource/menu-strings/translation/$lang/?file&mode=default"
echo "Downloading $filename"
curl -u "$username" -o "$filename" -b "$cookies" -k -s -L "$url"
done

git add ./res/values*/strings.xml
git commit ./res/values*/strings.xml -m "res: Updated translations."
