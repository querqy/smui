# syntax = docker/dockerfile:1.0-experimental
FROM openjdk:11-buster as builder

ARG NODE_VERSION=10
ARG SBT_VERSION=1.5.6

RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get -y install sbt

RUN apt-get install -y lsb-release \
    && export DISTRO="$(lsb_release -s -c)" \
    && echo "deb https://deb.nodesource.com/node_$NODE_VERSION.x $DISTRO main" > /etc/apt/sources.list.d/nodesource.list \
    && echo "deb-src https://deb.nodesource.com/node_$NODE_VERSION.x $DISTRO main" >> /etc/apt/sources.list.d/nodesource.list \
    && curl -sSL https://deb.nodesource.com/gpgkey/nodesource.gpg.key | apt-key add - \
    && apt-get update \
    && apt-get install -y nodejs

COPY . /smui
WORKDIR /smui

RUN --mount=target=/root/.ivy2,type=cache sbt "set test in assembly := {}" clean assembly

FROM openjdk:11-jre-slim-buster

RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-client sshpass bash curl git \
    && rm -rf /var/lib/apt/lists/*

ARG VERSION
ENV SMUI_VERSION=$VERSION

# PID file should be /dev/null in docker containers, as SMUI is the only process in the container, anyway
# and present PID files from previous runs prevent startup
ENV SMUI_CONF_PID_PATH=/dev/null
ENV SMUI_CONF_HTTP_PORT=9000
ENV SMUI_CONF_LOGBACK_XML_PATH=/smui/logback.xml

EXPOSE $SMUI_CONF_HTTP_PORT

# create non-root smui user & group (security)
RUN addgroup --gid 1024 smui \
    && adduser --uid 1024 --ingroup smui smui --disabled-password --quiet

WORKDIR /smui

RUN mkdir /tmp/smui-git-repo /home/smui/.ssh \
    && chown -R smui:smui /smui /tmp/smui-git-repo /home/smui/.ssh

USER smui

COPY --chown=smui:smui conf/logback.xml .
COPY --chown=smui:smui conf/smui2solr.sh conf/smui2git.sh conf/
COPY --from=builder --chown=smui:smui /smui/target/scala-*/search-management-ui-assembly-$VERSION.jar .

CMD java \
  -Dpidfile.path=$SMUI_CONF_PID_PATH \
  -Dlogback.configurationFile=$SMUI_CONF_LOGBACK_XML_PATH \
  -Dhttp.port=$SMUI_CONF_HTTP_PORT \
  -jar /smui/search-management-ui-assembly-$SMUI_VERSION.jar
