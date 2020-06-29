package com.akolar.eventproc.consumer

import java.util.Properties

import com.akolar.eventproc.{Config, Country}
import com.akolar.eventproc.consumer.MeetupEvent.{Event, Location}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.{TumblingEventTimeWindows, TumblingProcessingTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer, KafkaSerializationSchema}
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema

object MeetupJob {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(10000L)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val stream = env.addSource(getConsumer())

    val input = stream
      .map {v => v.get("value")}
      .filter {_.has("venue")}
      .filter {v => {
        val venue = v.get("venue")
        v.has("time") &&
          venue.has("country") &&
          venue.has("city") &&
          venue.has("lat") &&
          venue.has("lon")
      }}
      .map {v => {
          val venue = v.get("venue")
          val country = venue.get("country").asText.toUpperCase
          val city = venue.get("city").asText.toLowerCase
          val lat = venue.get("lat").asDouble
          val lon = venue.get("lon").asDouble
          val evTime = v.get("time").asLong

          new Event(country, city, new Location(lat, lon), evTime)
        }}
        .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[Event](Time.hours(1)){
          override def extractTimestamp(t: Event): Long = { t.EventTime }
        })

    input
      .filter {_.country == "DE"}
      .filter {v => {v.city == "munich" || v.city == "münchen" || v.city == "muenchen"}}
      .map {v => {
            val latBucket = (v.loc.lat / 0.009).toInt
            val lonBucket = (v.loc.lon / 0.0134).toInt
            (f"$latBucket,$lonBucket", 1)
        }}
      .keyBy(0)
      .sum(1)
      .addSink(getProducer(Config.KafkaMunichHotspotsTopic))

    input.filter { v => Country.inEurope(v.country) }
        .map {v => (v.city, 1)}
        .keyBy(0)
        .window(TumblingEventTimeWindows.of(Time.days(30)))
        .sum(1)
        .addSink(getProducer(Config.KafkaTopCitiesTopic))

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

  def getProducer(topic: String): FlinkKafkaProducer[(String, Int)] = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.put("transaction.max.timeout.ms", "900000")
    properties.put("transaction.timeout.ms", "600000")
    new FlinkKafkaProducer[(String, Int)](topic, new AggregateSerializer(topic), properties, Semantic.AT_LEAST_ONCE)
  }
}
