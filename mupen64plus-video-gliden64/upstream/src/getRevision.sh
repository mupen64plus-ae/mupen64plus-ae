SCRIPT_DIRECTORY=`dirname $0`
rev=\"`git rev-parse --short HEAD`\"
lastrev=$(cat $SCRIPT_DIRECTORY/revision.txt)

echo current revision $rev
echo last build revision $lastrev

if [ "$lastrev" != "$rev" ]
then
   echo "#define PLUGIN_REVISION $rev" > $SCRIPT_DIRECTORY/Revision.h
   echo "#define PLUGIN_REVISION_W L$rev" >> $SCRIPT_DIRECTORY/Revision.h
   echo "$rev" > $SCRIPT_DIRECTORY/revision.txt
fi
