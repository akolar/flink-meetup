FROM openjdk:14-alpine

WORKDIR /
ADD target/eventproc-1.0-SNAPSHOT-jar-with-dependencies.jar producer.jar
ADD data/events.json /data/events.json

CMD ["java", "-jar", "producer.jar"]
