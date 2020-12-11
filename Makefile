all:

serve:
	@sbt run

docker-build-only:
	DOCKER_BUILDKIT=1 sbt docker

docker-push:
	DOCKER_BUILDKIT=1 sbt docker dockerPush
