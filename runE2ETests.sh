#!/bin/bash

CYPRESS_VERSION=6.4.0
PROJECT_NAME=smui-integration-tests
READY_WAIT_TIME_SECONDS=30

echo "-> Build docker containers..."
make docker-build-only

echo "-> Finished building SMUI. Starting SMUI and db..."
docker-compose --project-name $PROJECT_NAME pull database
docker-compose --project-name $PROJECT_NAME up --build -d

STATUS="0"
LOOP_COUNTER=0
while [ "$STATUS" != "200" ] && [ $LOOP_COUNTER -lt $READY_WAIT_TIME_SECONDS ]
do
	sleep 1
	echo "-> Waiting for SMUI to become ready. Current status: $STATUS."
	STATUS=$(curl -sL -w "%{http_code}\\n" "http://localhost:9000/health" -o /dev/null)
	LOOP_COUNTER=$((LOOP_COUNTER+1))
done

if [ $LOOP_COUNTER -ge $READY_WAIT_TIME_SECONDS ]
then
	echo "-> SMUI did not start successfully in $READY_WAIT_TIME_SECONDS seconds, giving up."
	docker-compose --project-name $PROJECT_NAME logs
	exit 1
fi

echo "-> Installing cypress plugins ..."
cd e2e
npm install

echo "-> SMUI has started. Running tests..."
cd ..
docker run -it -v $PWD/e2e:/e2e -w /e2e --net=host cypress/included:$CYPRESS_VERSION

echo "-> Finished testing. Stopping docker container..."
docker-compose --project-name $PROJECT_NAME down --volumes
