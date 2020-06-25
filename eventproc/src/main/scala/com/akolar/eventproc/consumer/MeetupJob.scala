package com.akolar.eventproc.consumer

import java.util.Properties

import com.akolar.eventproc.Config
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema

object MeetupJob {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(10000L, CheckpointingMode.EXACTLY_ONCE)

    val stream = env.addSource(getConsumer())
    /* TODO:
     flatmap: start time, event time, country, city, lat, lon
      -> filter by country=de -> filter by city=munich -> split by lat,lon -> count
      -> filter by country in EU -> group by city -> count
     */

    env.execute("meetup pipeline")
  }

  def getConsumer(): FlinkKafkaConsumer[ObjectNode] = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.setProperty("group.id", Config.KafkaConsumerGroup)

    val consumer = new FlinkKafkaConsumer(
      Config.KafkaMeetupEventsTopic,
      new JSONKeyValueDeserializationSchema(false),
      properties)
    consumer.setStartFromEarliest()

    consumer
  }
}
