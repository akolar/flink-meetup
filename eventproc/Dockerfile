FROM openjdk:14-alpine

RUN apk add --update curl tar gzip jq xz \
    && rm -rf /var/cache/apk/*

# Maven
RUN mkdir -p /usr/local/lib/maven
WORKDIR /usr/local/lib/maven
RUN curl -s https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -o maven.tar.gz
RUN tar xvzf maven.tar.gz --strip=1 >> /dev/null
ENV PATH="/usr/local/lib/maven/bin:${PATH}"

# Data
RUN mkdir /data
WORKDIR /data
RUN curl -s http://akolar-fs.s3.eu-central-1.amazonaws.com/kafka/events-gen.json.xz -o events.json.xz
RUN xz -d -T0 events.json.xz

# Compile
RUN mkdir -p /source
WORKDIR /source
ADD pom.xml /source/pom.xml
RUN mvn install -q
ADD src /source/src
RUN mvn clean package -q

# App
WORKDIR /
RUN cp /source/target/producer-jar-with-dependencies.jar producer.jar
RUN cp /source/target/consumer-jar-with-dependencies.jar consumer.jar
RUN rm -rf /source
ADD run.sh /run.sh

CMD ["sh", "-c", "sh run.sh ${MODE}"]
