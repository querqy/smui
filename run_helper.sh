#!/bin/bash
source ~/.zshrc
export SMUI_TOGGLE_SPELLING=true
export SMUI_TOGGLE_EVENTHISTORY=true
#sbt "run -Dconfig.file=./smui-dev.conf 9000"
#sbt "run -Dconfig.file=./smui-dev.conf 9000"

#sbt run "-Dconfig.file=./smui-dev.conf 9000"

sbt -jvm-debug 9998 run "-Dconfig.file=./smui-dev.conf"
# 9000"

#gopen http://localhost:9000