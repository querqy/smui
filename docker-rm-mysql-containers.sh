#!/bin/bash
docker ps -a -q --filter "ancestor=mysql:5.7" | xargs -I {} docker container stop {}
docker ps -a -q --filter "ancestor=mysql:5.7" | xargs -I {} docker container rm {}
