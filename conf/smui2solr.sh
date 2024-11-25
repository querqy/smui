#!/bin/bash

set -euo pipefail

# Expected command line parameters
common_rules_file=$1
decompound_rules_file="${common_rules_file}-2"
replace_rules_file=$2
target_system=$3
solr_core_or_collection_name=$4

# Read in environment variables

# Querqy rewriter names for rules, (optionally when exported separately) the decompound rules and spelling/replacement rules
common_rules_rewriter_name="${SMUI_QUERQY_REWRITER_COMMON_RULES:-common_rules}"
common_rules_decompound_rewriter_name="${SMUI_QUERQY_REWRITER_COMMON_RULES_DECOMPOUND:-common_rules_decompound}"
replace_rules_rewriter_name="${SMUI_QUERQY_REWRITER_REPLACE:-replace_rules}"

# (legacy) the live Solr host the rules should be deployed to
SMUI_2SOLR_SOLR_HOST=${SMUI_2SOLR_SOLR_HOST:-}

# (alternatively) the live Solr host the rules should be deployed to
SMUI_DEPLOY_LIVE_SOLR_HOST=${SMUI_DEPLOY_LIVE_SOLR_HOST:-}

# (optional, if basic authentication is used): Solr user for non-prelive deployment
SMUI_DEPLOY_LIVE_SOLR_USER=${SMUI_DEPLOY_LIVE_SOLR_USER:-}

# (optional, if basic authentication is used): Solr password for non-prelive deployment
SMUI_DEPLOY_LIVE_SOLR_PASSWORD=${SMUI_DEPLOY_LIVE_SOLR_PASSWORD:-}

# (optional): whether certificate validation should be turned off when connecting to (live) Solr host
SMUI_DEPLOY_LIVE_SOLR_ALLOW_INSECURE=${SMUI_DEPLOY_LIVE_SOLR_ALLOW_INSECURE:-}

# (optional, if PRELIVE is used as a deployment target): the prelive Solr host the rules should be deployed to
SMUI_DEPLOY_PRELIVE_SOLR_HOST=${SMUI_DEPLOY_PRELIVE_SOLR_HOST:-}

# (optional, if basic authentication is used): Prelive Solr user
SMUI_DEPLOY_PRELIVE_SOLR_USER=${SMUI_DEPLOY_PRELIVE_SOLR_USER:-}

# (optional, if basic authentication is used): Prelive Solr user
SMUI_DEPLOY_PRELIVE_SOLR_PASSWORD=${SMUI_DEPLOY_PRELIVE_SOLR_PASSWORD:-}

# (optional): whether certificate validation should be turned off when connecting to prelive Solr host
SMUI_DEPLOY_PRELIVE_SOLR_ALLOW_INSECURE=${SMUI_DEPLOY_PRELIVE_SOLR_ALLOW_INSECURE:-}

# Solr (basic) authentication
solr_host=""
solr_basic_auth_user=""
solr_basic_auth_password=""
curl_solr_insecure_flag=""

if [ "${target_system}" == "PRELIVE" ] ;
then
  solr_host="${SMUI_DEPLOY_PRELIVE_SOLR_HOST:-}"
  if [ -n "${SMUI_DEPLOY_PRELIVE_SOLR_USER}" ] && [ -n "${SMUI_DEPLOY_PRELIVE_SOLR_PASSWORD}" ]; then
    solr_basic_auth_user=${SMUI_DEPLOY_PRELIVE_SOLR_USER}
    solr_basic_auth_password=${SMUI_DEPLOY_PRELIVE_SOLR_PASSWORD}
  fi
  if [ "${SMUI_DEPLOY_PRELIVE_SOLR_ALLOW_INSECURE,,}" == "true" ] ;
  then
    curl_solr_insecure_flag="--insecure"
  fi
fi

if [ "${target_system}" == "LIVE" ] ;
then
  solr_host="${SMUI_2SOLR_SOLR_HOST:-${SMUI_DEPLOY_LIVE_SOLR_HOST:-}}"
  if [ -n "${SMUI_DEPLOY_LIVE_SOLR_USER}" ] && [ -n "${SMUI_DEPLOY_LIVE_SOLR_PASSWORD}" ]; then
    solr_basic_auth_user=${SMUI_DEPLOY_LIVE_SOLR_USER}
    solr_basic_auth_password=${SMUI_DEPLOY_LIVE_SOLR_PASSWORD}
  fi
  if [ "${SMUI_DEPLOY_LIVE_SOLR_ALLOW_INSECURE,,}" == "true" ] ;
  then
    curl_solr_insecure_flag="--insecure"
  fi
fi

save_rewriter() {
  local querqy_rewriter_name="$1"
  local querqy_rewriter_data="$2"

  # read into array
  IFS=' ' read -r -a curl_command <<< "curl -X POST -H \"Content-Type: application/json\" --fail"

  # add payload
  curl_command+=("-d" "${querqy_rewriter_data}")

  # optional basic auth username + password and curl --insecure flag
  if [ -n "${solr_basic_auth_user}" ] && [ -n "${solr_basic_auth_password}" ]; then
    curl_command+=("--user" "${solr_basic_auth_user}:${solr_basic_auth_password}")
  fi
  if [ -n "${curl_solr_insecure_flag:-}" ]; then
    curl_command+=("${curl_solr_insecure_flag}")
  fi

  # add url
  curl_command+=("${solr_host}/solr/${solr_core_or_collection_name}/querqy/rewriter/${querqy_rewriter_name}?action=save")

  echo "${curl_command[@]}"

  "${curl_command[@]}"
}

echo "In smui2solr.sh - script for updating rules in Solr using an API call to the Querqy (>=v5) plugin "
echo "^-- common_rules_file = ${common_rules_file}"
echo "^-- decompound_rules_file = ${common_rules_file}-2"
echo "^-- solr_host = ${solr_host}"
echo "^-- target_system: ${target_system}"
echo "^-- solr_core_or_collection_name = ${solr_core_or_collection_name}"

# DEPLOYMENT
#####

# common rules
if [ -s "${common_rules_file}" ] && [ -n "${common_rules_rewriter_name}" ]; then
  # build the rewriter request payload by embedding the rules text into JSON
  common_rules_rewriter_data=$(jq -Rs '{class: "querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory", config: { rules: . }}' "${common_rules_file}")
  echo "Deploying common rules for core ${solr_core_or_collection_name} to rewriter ${common_rules_rewriter_name} at ${solr_host}..."
  save_rewriter "${common_rules_rewriter_name}" "${common_rules_rewriter_data}"
fi

# optional: decompound rules to a separate common rules rewriter
if [ -s "${decompound_rules_file}" ] && [ -n "${common_rules_decompound_rewriter_name}" ]; then
  common_rules_decompound_rewriter_data=$(jq -Rs '{class: "querqy.solr.rewriter.commonrules.CommonRulesRewriterFactory", config: { rules: . }}' "${decompound_rules_file}")
  echo "Deploying decompound common rules for core ${solr_core_or_collection_name} to rewriter ${common_rules_decompound_rewriter_name} at ${solr_host}..."
  save_rewriter "${common_rules_decompound_rewriter_name}" "${common_rules_decompound_rewriter_data}"
fi

# deploy replace / spelling correction rules to a separate replace rewriter if they exist
if [ -s "${replace_rules_file}" ] && [ -n "${replace_rules_rewriter_name}" ]; then
  replace_rules_rewriter_data=$(jq -Rs '{class: "querqy.solr.rewriter.replace.ReplaceRewriterFactory", config: { ignoreCase: true, inputDelimiter: ";", rules: . }}' "${replace_rules_file}")
  echo "Deploying replace rules for core ${solr_core_or_collection_name} to rewriter ${replace_rules_rewriter_name} at ${solr_host}..."
  save_rewriter "${replace_rules_rewriter_name}" "${replace_rules_rewriter_data}"
fi

echo "Done"
