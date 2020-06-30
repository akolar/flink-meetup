package com.akolar.eventproc.consumer

import java.lang

import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema
import org.apache.kafka.clients.producer.ProducerRecord

class RankSerializer(topic: String) extends KafkaSerializationSchema[(String, Int, String, Int)] {
  override def serialize(el: (String, Int, String, Int), ts: lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    new ProducerRecord[Array[Byte], Array[Byte]](this.topic, el._1.getBytes, el.toString.getBytes)
  }
}
