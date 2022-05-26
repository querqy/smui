#!/bin/bash

# make sure to start docker desktop first

docker run --name smui-mysql -e MYSQL_ROOT_PASSWORD=smui -e MYSQL_USER=smui -e MYSQL_PASSWORD=smui -e MYSQL_DATABASE=smui -p 3306:3306 -d mysql:5.7