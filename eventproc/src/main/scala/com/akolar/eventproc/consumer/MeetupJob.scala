package com.akolar.eventproc.consumer

import java.util.Properties

import com.akolar.eventproc.{Config, Country}
import com.akolar.eventproc.consumer.MeetupEvent.{Event, Location}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema

object MeetupJob {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(10000L)

    val stream = env.addSource(getConsumer())
    /* TODO:
     flatmap: start time, event time, country, city, lat, lon
      -> filter by country=de -> filter by city=munich -> split by lat,lon -> count
      -> filter by country in EU -> group by city -> count
     */

    val input = stream.map {v => {
      val venue = v.get("venue")
      val country = venue.get("country").asText.toLowerCase
      val city = venue.get("city").asText
      val lat = venue.get("lat").asDouble
      val lon = venue.get("lon").asDouble
      val evTime = v.get("time").asLong

      new Event(country, city, new Location(lat, lon), System.currentTimeMillis, evTime)
    }}

    input.filter {_.country == "de"}
        .filter {_.country == "Munich"}
        .map {v => ((v.loc.lat / 0.009).toInt, (v.loc.lon / 0.0134).toInt, 1)}
        .keyBy(0).keyBy(1)
        .sum(2)
        .print()

    input.filter { v => Country.inEurope(v.country) }
        .map(v => (v.city, 1))
        .keyBy(0)
        .sum(1)
        .print()

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
