echo "Transifex username:"
read username

# Remember session using cookie
# Doesn't seem to be working, Transifex probably uses javascript-based login
cookies="cookies.tmp"
curl -u "$username" -o "/dev/null" -c "$cookies" -k -s -L "https://www.transifex.com/signin/"

# Download the files by iterating through the language codes
for lang in "de" "es" "fr" "gl" "hr" "it" "ja" "nb" "nl" "pl" "pt" "ru" "tr"
do
filename="res/values-$lang/strings.xml"
url="https://www.transifex.com/api/2/project/mupen64plus-ae/resource/menu-strings/translation/$lang/?file&mode=default"
echo "Downloading $filename"
curl -u "$username" -o "$filename" -b "$cookies" -k -s -L "$url"
done
