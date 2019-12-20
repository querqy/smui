# syntax = tonistiigi/dockerfile:runmount20180607

# Build SMUI using a multi-stage build

FROM centos:7 as builder

ENV container docker

RUN yum update -y
RUN yum -y install java-1.8.0-openjdk-devel
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo
RUN yum -y install sbt
RUN curl --silent --location https://rpm.nodesource.com/setup_10.x | bash -
RUN yum -y install nodejs

RUN yum clean all

RUN npm i graceful-fs
RUN npm i resolve
RUN npm i esutils
RUN npm i semver

COPY . /smui
WORKDIR /smui

RUN --mount=target=/root/.ivy2,type=cache sbt "set test in assembly := {}" clean assembly


# Finally produce slim  application image

FROM openjdk:8-alpine

# TODO evaluate other deployment approaches (for remote targets) than sshpass + scp
RUN apk update
RUN apk upgrade
RUN apk add --update --no-cache sshpass
RUN apk add --update --no-cache openssh sshpass
RUN apk add bash
RUN apk add curl

ARG VERSION
ARG SCALA_VERSION
ENV SMUI_VERSION=$VERSION

WORKDIR /smui

COPY --from=builder /smui/target/scala-$SCALA_VERSION/search-management-ui-assembly-$VERSION.jar .
COPY conf/logback.xml /smui/logback.xml
COPY conf/smui2solr.sh /smui/conf/smui2solr.sh

ENV SMUI_CONF_PID_PATH=/var/run/play.pid
ENV SMUI_CONF_LOG_BASE_PATH=/var/log
ENV SMUI_CONF_HTTP_PORT=9000
ENV SMUI_CONF_LOGBACK_XML_PATH=/smui/logback.xml

EXPOSE $SMUI_CONF_HTTP_PORT

CMD java \
  -Dpidfile.path=$SMUI_CONF_PID_PATH \
  -DLOG_BASE_PATH=$SMUI_CONF_LOG_BASE_PATH \
  -Dlogback.configurationFile=$SMUI_CONF_LOGBACK_XML_PATH \
  -Dhttp.port=$SMUI_CONF_HTTP_PORT \
  -jar /smui/search-management-ui-assembly-$SMUI_VERSION.jar
