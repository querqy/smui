#!/bin/bash

set -euo pipefail

# interface variables
SRC_TMP_FILE=$1
SMUI_GIT_REPOSITORY=$2
SMUI_GIT_FN_COMMON_RULES_TXT=$3

# helper variables
SMUI_GIT_PATH=${SMUI_GIT_PATH:-/}
SMUI_GIT_BRANCH=${SMUI_GIT_BRANCH:-master}
SMUI_GIT_COMMIT_MSG=${SMUI_GIT_COMMIT_MSG:-"Update from SMUI-bot (with smui2git.sh)"}
SMUI_GIT_CLONE_PATH=${SMUI_GIT_CLONE_PATH:-/tmp/smui-git-repo}

# configure git
export GIT_TRACE=2

_log() {
  echo -e "${1}"
}

_info() {
  _log "In smui2git.sh - script performing rules.txt update to git repo"
  _log "^-- SRC_TMP_FILE:                 ${SRC_TMP_FILE}"
  _log "^-- SMUI_GIT_REPOSITORY:          ${SMUI_GIT_REPOSITORY}"
  _log "^-- SMUI_GIT_FN_COMMON_RULES_TXT: ${SMUI_GIT_FN_COMMON_RULES_TXT}"
  _log "^-- SMUI_GIT_PATH:                ${SMUI_GIT_PATH}"
  _log "^-- SMUI_GIT_BRANCH:              ${SMUI_GIT_BRANCH}"
  _log "^-- SMUI_GIT_COMMIT_MSG:          ${SMUI_GIT_COMMIT_MSG}"
  _log "^-- SMUI_GIT_CLONE_PATH:          ${SMUI_GIT_CLONE_PATH}"
  _log ""
}

_initGit() {
  _log "remove old git directory"
  rm -Rf "${SMUI_GIT_CLONE_PATH}"
  if [ ! -d "${SMUI_GIT_CLONE_PATH}/.git" ]; then
    _log "Clone GitRepository ${SMUI_GIT_REPOSITORY} to ${SMUI_GIT_CLONE_PATH}"
    git clone "${SMUI_GIT_REPOSITORY}" "${SMUI_GIT_CLONE_PATH}"
  fi
}

_publishToGit() {
  if [ ! -d "${SMUI_GIT_CLONE_PATH}/.git" ]; then
    _log "Git Directory not exists"
    exit 1
  fi;

  _log "git checkout -f ${SMUI_GIT_BRANCH}"
  git -C "${SMUI_GIT_CLONE_PATH}" checkout -f "${SMUI_GIT_BRANCH}"

  _log "git pull --prune"
  git -C "${SMUI_GIT_CLONE_PATH}" pull --prune

  _log "cp ${SRC_TMP_FILE} ${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}${SMUI_GIT_FN_COMMON_RULES_TXT}"
  cp "${SRC_TMP_FILE}" "${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}${SMUI_GIT_FN_COMMON_RULES_TXT}"

  _log "git add ${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}${SMUI_GIT_FN_COMMON_RULES_TXT}"
  git -C "${SMUI_GIT_CLONE_PATH}" add "${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}${SMUI_GIT_FN_COMMON_RULES_TXT}"

  _log "git commit -m ${SMUI_GIT_COMMIT_MSG}"
  git -C "${SMUI_GIT_CLONE_PATH}" commit -m "${SMUI_GIT_COMMIT_MSG}"

  _log "git push"
  git -C "${SMUI_GIT_CLONE_PATH}" push
}

_info
_initGit
_publishToGit

exit 0;
