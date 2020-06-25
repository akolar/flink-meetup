package com.akolar.eventproc.producer

import java.util.Properties
import java.security.MessageDigest
import java.math.BigInteger

import com.akolar.eventproc.Config
import org.apache.kafka.clients.producer._

import scala.io.Source

object Producer {
  def main(args: Array[String]): Unit = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("retries", "100")
    properties.put("retry.backoff.ms", "100")
    properties.put("max.in.flight.requests.per.connection", "1")
    properties.put("acks", "all")

    val producer = new KafkaProducer[String, String](properties)

    for (i <- 1 to Config.ProducerIterations) {
      printf("Producing epoch %d/%d... ", i, Config.ProducerIterations)
      Thread.sleep(2000L)
      generateMessages(producer)
      println("Done")
    }

    producer.flush()
    producer.close()
  }

  def generateMessages(producer: KafkaProducer[String, String]): Unit = {
    val data = Source.fromFile(Config.InputFile)
    for (line <- data.getLines) {
      val record = new ProducerRecord[String, String](Config.KafkaMeetupEventsTopic, md5sum(line), line)
      producer.send(record)
    }
    data.close()
  }

  def md5sum(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1,digest)
    bigInt.toString(16)
  }
}
