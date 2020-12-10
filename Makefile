all: docker-build-only

docker-build-only:
	DOCKER_BUILDKIT=1 sbt docker

docker-push:
	DOCKER_BUILDKIT=1 sbt docker dockerPush
