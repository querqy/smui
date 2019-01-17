#!/usr/bin/env bash

yum update -y

# TODO command doesnt seem to install rpm-build properly :-(
yum -y install rpm-build

yum -y install java-1.8.0-openjdk-devel

curl https://bintray.com/sbt/rpm/rpm | sudo tee /etc/yum.repos.d/bintray-sbt-rpm.repo

yum -y install sbt

curl --silent --location https://rpm.nodesource.com/setup_10.x | bash -

yum -y install nodejs

#yum -y install wget

# TODO Install MariaDB (version 5.5.56)
## ... the repo file
# vi /etc/yum.repos.d/MariaDB.repo
## [mariadb]
## name = MariaDB-5.5.56
## baseurl=https://downloads.mariadb.com/files/MariaDB/mariadb-5.5.56/yum/rhel6-amd64/
## # alternative: baseurl=http://archive.mariadb.org/mariadb-5.5.56/yum/rhel6-amd64/
## gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
## gpgcheck=1
## ... the command
# sudo yum -y install MariaDB-server MariaDB-client
## ... command needed to be redone (after removing the lib component) - due to my.conf version conflict :-( <== TODO!
# sudo yum -y remove mariadb-libs-5.5.56-2.el7.x86_64
# sudo yum -y install MariaDB-server MariaDB-client

# TODO create an according database (and MariaDB user) and grant according permissions
## CREATE USER 'smui'@'localhost' IDENTIFIED BY 'smui';
## CREATE DATABASE smui;
## GRANT ALL PRIVILEGES ON smui.* TO 'smui'@'localhost' WITH GRANT OPTION;

#cd /tmp
#wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u60-b27/jdk-8u60-linux-x64.rpm"
 
#rpm -Uvh jdk-8u60-linux-x64.rpm
 
#rm jdk-8u60-linux-x64.rpm

#apt-get install -y openjdk-7-jdk 

#add-apt-repository ppa:webupd8team/java

#echo "deb https://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list
#apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
#echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections

#apt-get update

#apt-get install oracle-java8-installer

#echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list

#apt-get install -y sbt

#add-apt-repository ppa:webupd8team/java

#apt-get update

#apt-get install -y oracle-java8-installer

#apt-get install -y nodejs

if ! grep -Fq "sbt" /home/vagrant/.profile; then
  echo 'export SBT_OPTS="-Dsbt.jse.engineType=Node -Dsbt.jse.command=$(which nodejs)"' >> /home/vagrant/.profile
fi
