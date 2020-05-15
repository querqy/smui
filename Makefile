
IMAGE = querqy/smui
VERSION = $(shell grep -E '^version' build.sbt | cut -d'"' -f2)
SCALA_VERSION = $(shell grep -E '^scalaVersion' build.sbt | cut -d'"' -f2 | cut -d. -f1-2)

all: docker-build-only

docker-build-only:
	env DOCKER_BUILDKIT=1 docker build --build-arg VERSION=$(VERSION) \
		--build-arg SCALA_VERSION=$(SCALA_VERSION) -t $(IMAGE):$(VERSION) -f build/Dockerfile .

docker-run:
	mkdir -p var
	docker run -v`pwd`/var:/var/smui -p9000:9000 --rm -it --name smui \
		-eSMUI_DB_URL=jdbc:sqlite:/var/smui/test.db -eSMUI_DB_JDBC_DRIVER=org.sqlite.JDBC \
		$(IMAGE):$(VERSION)

docker-push: docker-push-version docker-push-latest

docker-push-version:
	docker push $(IMAGE):$(VERSION)

docker-push-latest:
	docker tag $(IMAGE):$(VERSION) $(IMAGE):latest
	docker push $(IMAGE):latest
