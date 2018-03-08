SRC_TMP_FILE=$1
SOLR_CORE_NAME=$2
DST_CP_FILE_TO="/todo/path/to/rules.txt"
SOLR_HOST="localhost:8983"

echo "=== smui2solr script performing rules.txt update and core reload ==="
echo "SRC_TMP_FILE: $SRC_TMP_FILE"
echo "SOLR_CORE_NAME: $SOLR_CORE_NAME"
echo "Solr URL call: http://$SOLR_HOST/solr/admin/cores?wt=xml&action=RELOAD&core=$SOLR_CORE_NAME"
echo "=== Hope for the best ;-) Cheers Mate! ==="

cp $SRC_TMP_FILE $DST_CP_FILE_TO

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
