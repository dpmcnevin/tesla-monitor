FROM openjdk:11-jdk-slim as build

RUN apt-get update && apt-get -y install maven

COPY . /var/app
WORKDIR /var/app
RUN mvn clean package

FROM openjdk:11-jre-slim

WORKDIR /var/app
COPY --from=build /var/app/target/shaded-tesla-1.0-SNAPSHOT.jar .

CMD java -cp shaded-tesla-*-SNAPSHOT.jar net.corrupt.stats.Application