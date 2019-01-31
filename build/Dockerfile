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

COPY . /smui
WORKDIR /smui

RUN --mount=target=/root/.ivy2,type=cache sbt assembly


# Finally produce slim  application image

FROM openjdk:8-alpine

WORKDIR /smui

COPY --from=builder /smui/target/scala-2.12/search-management-ui-assembly-2.0.1.jar .

EXPOSE 9000

CMD ["java", "-jar", "/smui/search-management-ui-assembly-2.0.1.jar"]
