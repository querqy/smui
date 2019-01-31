
IMAGE = pbartusch/smui
VERSION = $(shell grep -E '^version' build.sbt | cut -d'"' -f2)

all: docker-build

docker-build:
	env DOCKER_BUILDKIT=1 docker build -t $(IMAGE):$(VERSION) -f build/Dockerfile .

docker-run:
	mkdir -p var
	docker run -v$(pwd)/var:/var/smui -p9000:9000 --rm -it --name smui \
		-eSMUI_DB_URL=jdbc:sqlite:/var/smui/test.db -eSMUI_DB_JDBC_DRIVER=org.sqlite.JDBC \
		$(IMAGE):$(VERSION)

docker-push-version:
	docker push $(IMAGE):$(VERSION)

docker-push-latest:
	docker tag $(IMAGE):$(VERSION) $(IMAGE):latest
	docker push $(IMAGE):latest
