#!/bin/bash

set -euo pipefail

SRC_TMP_FILE=$1
DST_CP_FILE_TO=$2
SOLR_HOST=$3
SOLR_CORE_NAME=$4
DECOMPOUND_DST_CP_FILE_TO=$5
TARGET_SYSTEM=$6
REPLACE_RULES_SRC_TMP_FILE=$7
REPLACE_RULES_DST_CP_FILE_TO=$8

echo "In smui2solr.sh - script performing rules.txt update and core reload"
echo "^-- SRC_TMP_FILE = $SRC_TMP_FILE"
echo "^-- DST_CP_FILE_TO = $DST_CP_FILE_TO"
echo "^-- SOLR_HOST = $SOLR_HOST"
echo "^-- SOLR_CORE_NAME: $SOLR_CORE_NAME"
echo "^-- DECOMPOUND_DST_CP_FILE_TO = $DECOMPOUND_DST_CP_FILE_TO"
echo "^-- TARGET_SYSTEM = $TARGET_SYSTEM"
echo "^-- REPLACE_RULES_SRC_TMP_FILE = $REPLACE_RULES_SRC_TMP_FILE"
echo "^-- REPLACE_RULES_DST_CP_FILE_TO = $REPLACE_RULES_DST_CP_FILE_TO"

# DEPLOYMENT
#####

echo "^-- Perform rules.txt deployment (decompound-rules.txt eventually)"

# $1 - from_filename
# $2 - to_filename (might be local or remote)
function deploy_rules_txt {
	# e.g.: remote_user:remote_pass@remote_host:/path/to/live/solr/defaultCore/conf/rules.txt
	if [[ $2 =~ (.*):(.*)@(.*) ]]
	then
		echo "^-- ... matched remote target (regex). proceeding with remote copy ..."
		sshpass -p "${BASH_REMATCH[2]}" scp -v -o StrictHostKeyChecking=no $1 ${BASH_REMATCH[1]}@${BASH_REMATCH[3]}
	else
		echo "^-- ... no match (regex). proceeding with regular cp ..."
		cp $1 $2
	fi
}

echo "^-- ... rules.txt"
deploy_rules_txt $SRC_TMP_FILE $DST_CP_FILE_TO

echo "^-- ... decompound-rules.txt"
if ! [[ $DECOMPOUND_DST_CP_FILE_TO == "NONE" ]]
then
    deploy_rules_txt "$SRC_TMP_FILE-2" $DECOMPOUND_DST_CP_FILE_TO
fi

echo "^-- ... replace-rules.txt"
if ! [[ $REPLACE_RULES_SRC_TMP_FILE == "NONE" && $REPLACE_RULES_DST_CP_FILE_TO == "NONE" ]]
then
    deploy_rules_txt $REPLACE_RULES_SRC_TMP_FILE $REPLACE_RULES_DST_CP_FILE_TO
fi

# CORE RELOAD
#####

echo "^-- Perform core reload for SOLR_HOST = $SOLR_HOST, SOLR_CORE_NAME = $SOLR_CORE_NAME"

if ! [[ $SOLR_HOST == "NONE" ]]
then
	# TODO only core reload over http possible. make configurable.
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
fi

# all ok
echo "smui2solr.sh - ok"
exit 0
