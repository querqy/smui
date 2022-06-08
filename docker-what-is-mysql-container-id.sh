#!/bin/bash
docker ps -a -q --filter "ancestor=mysql:5.7"  