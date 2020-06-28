package com.akolar.eventproc.consumer

import java.lang

import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema
import org.apache.kafka.clients.producer.ProducerRecord

class AggregateSerializer(topic: String) extends KafkaSerializationSchema[(String, Int)] {
  override def serialize(el: (String, Int), ts: lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    val body = f"${el._1},${el._2}"
    new ProducerRecord[Array[Byte], Array[Byte]](this.topic, el._1.getBytes, body.getBytes)
  }
}
