package com.akolar.eventproc.producer

import java.util.Properties

import com.akolar.eventproc.Config
import org.apache.kafka.clients.producer._

import scala.io.Source

object Producer {
  def main(args: Array[String]): Unit = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    val producer = new KafkaProducer[Integer, String](properties)
    for (i <- 1 to Config.ProducerIterations) {
      generateMessages(producer)
    }
  }

  def generateMessages(producer: KafkaProducer[Integer, String]): Unit = {
    val rand = util.Random

    for (line <- Source.fromFile(Config.InputFile).getLines) {
      val record = new ProducerRecord[Integer, String](Config.KafkaMeetupEventsTopic, rand.nextInt, line)
      producer.send(record)
    }
  }
}
