#!/bin/bash
source ~/.zshrc
export SMUI_TOGGLE_SPELLING=true
#sbt "run -Dconfig.file=./smui-dev.conf 9000"
#sbt "run -Dconfig.file=./smui-dev.conf 9000"
sbt run "-Dconfig.file=./smui-dev.conf 9000"
#gopen http://localhost:9000