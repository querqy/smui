SRC_TMP_FILE=$1
DST_CP_FILE_TO=$2
SOLR_HOST=$3
SOLR_CORE_NAME=$4
DECOMPOUND_DST_CP_FILE_TO=$5
TARGET_SYSTEM=$6

echo "In smui2solr.sh - script performing rules.txt update and core reload"
echo ":: SRC_TMP_FILE = $SRC_TMP_FILE"
echo ":: DST_CP_FILE_TO = $DST_CP_FILE_TO"
echo ":: SOLR_HOST = $SOLR_HOST"
echo ":: SOLR_CORE_NAME: $SOLR_CORE_NAME"
echo ":: DECOMPOUND_DST_CP_FILE_TO = $DECOMPOUND_DST_CP_FILE_TO"
echo ":: TARGET_SYSTEM = $TARGET_SYSTEM"

cp $SRC_TMP_FILE $DST_CP_FILE_TO

if ! [[ $DECOMPOUND_DST_CP_FILE_TO == "NONE" ]]
then
    cp "$SRC_TMP_FILE-2" $DECOMPOUND_DST_CP_FILE_TO
fi

SOLR_STATUS=$(curl -s -i -XGET "http://$SOLR_HOST/solr/admin/cores?wt=xml&action=RELOAD&core=$SOLR_CORE_NAME")

if [ $? -ne 0 ]; then
    exit 16
fi

if ! [[ $SOLR_STATUS ==  *"200 OK"* ]]
then 
    >&2 echo "Error reloading Solr core: $SOLR_STATUS"
    exit 17
fi

if ! [[ $SOLR_STATUS ==  *"<int name=\"status\">0</int>"* ]]
then
    >&2 echo "Error reloading Solr core: $SOLR_STATUS"
    exit 18
fi

exit 0
