FROM openjdk:14-alpine

# Sample data
ADD data/events1k.json /data/events.json

# Maven
RUN apk add --update curl tar gzip \
    && rm -rf /var/cache/apk/*
RUN mkdir -p /usr/local/lib/maven
WORKDIR /usr/local/lib/maven
RUN curl -s https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -o maven.tar.gz
RUN tar xvzf maven.tar.gz --strip=1 >> /dev/null
ENV PATH="/usr/local/lib/maven/bin:${PATH}"

# Compile
RUN mkdir -p /source
WORKDIR /source
ADD pom.xml /source/pom.xml
RUN mvn install -q
ADD src /source/src
RUN mvn package -q

# App
WORKDIR /
RUN cp /source/target/eventproc-1.0-jar-with-dependencies.jar producer.jar
RUN rm -rf /source

CMD ["java", "-jar", "producer.jar"]
