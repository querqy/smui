#!/bin/bash
set -e

SRC_TMP_FILE=$1
#SMUI_GIT_REPOSITORY=${SMUI_GIT_REPOSITORY};
SMUI_GIT_PATH=${SMUI_GIT_PATH:-/};
SMUI_GIT_BRANCH=${SMUI_GIT_BRANCH:-master};
SMUI_GIT_COMMIT_MSG=${SMUI_GIT_COMMIT_MSG:-"Updated from smui with smui2git.sh"}
SMUI_GIT_CLONE_PATH=${SMUI_GIT_CLONE_PATH:-/tmp/smui-git-repo}

_log() {
  echo -e "\033[0;33m${1}\033[0m"
}

_info() {
  _log "In smui2git.sh - script performing rules.txt update on git repo"
  _log "^-- SRC_TMP_FILE:        ${SRC_TMP_FILE}"
  _log "^-- SMUI_GIT_REPOSITORY: ${SMUI_GIT_REPOSITORY}"
  _log "^-- SMUI_GIT_PATH:       ${SMUI_GIT_PATH}"
  _log "^-- SMUI_GIT_BRANCH:     ${SMUI_GIT_BRANCH}"
  _log "^-- SMUI_GIT_COMMIT_MSG: ${SMUI_GIT_COMMIT_MSG}"
  _log "^-- SMUI_GIT_CLONE_PATH: ${SMUI_GIT_CLONE_PATH}"
  _log ""
}

_initGit() {
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

  _log "cp ${SRC_TMP_FILE} ${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}"
  cp "${SRC_TMP_FILE}" "${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}"

  _log "git add ${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}"
  git -C "${SMUI_GIT_CLONE_PATH}" add "${SMUI_GIT_CLONE_PATH}${SMUI_GIT_PATH}"

  _log "git commit -m ${SMUI_GIT_COMMIT_MSG}"
  git -C "${SMUI_GIT_CLONE_PATH}" commit -m "${SMUI_GIT_COMMIT_MSG}"

  _log "git push"
  git -C "${SMUI_GIT_CLONE_PATH}" push
}

_info
_initGit
_publishToGit

exit 0;
