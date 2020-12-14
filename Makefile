all:

serve:
	@sbt run

docker-build-only:
	DOCKER_BUILDKIT=1 sbt docker

docker-push:
	DOCKER_BUILDKIT=1 sbt dockerPush

docker-build-and-push:
	DOCKER_BUILDKIT=1 sbt dockerBuildAndPush

docker-run:
	@docker-compose up