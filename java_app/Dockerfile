FROM openjdk:11-slim

WORKDIR /var/app
RUN apt-get update && apt-get -y install maven

COPY pom.xml pom.xml
RUN mvn install