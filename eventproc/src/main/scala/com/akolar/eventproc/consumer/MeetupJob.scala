package com.akolar.eventproc.consumer

import java.util.Properties

import com.akolar.eventproc.{Config, Country}
import com.akolar.eventproc.consumer.MeetupEvent.{Event, Location}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema

object MeetupJob {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(10000L)
    env.setParallelism(Config.FlinkParallelism)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val stream = env.addSource(getConsumer)

    val input = stream
      .map { v => v.get("value") }
      .filter { _.has("venue") }
      .filter { v =>
        {
          val venue = v.get("venue")
          v.has("time") &&
          venue.has("country") &&
          venue.has("city") &&
          venue.has("lat") &&
          venue.has("lon")
        }
      }
      .map { v =>
        {
          val venue   = v.get("venue")
          val country = venue.get("country").asText.toUpperCase
          val city    = venue.get("city").asText.toLowerCase
          val lat     = venue.get("lat").asDouble
          val lon     = venue.get("lon").asDouble
          val evTime  = v.get("time").asLong

          new Event(country, city, new Location(lat, lon), evTime)
        }
      }
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[Event](Time.hours(1)) {
        override def extractTimestamp(t: Event): Long = { t.EventTime }
      })

    input
      .filter { _.country == "DE" }
      .filter { v => { v.city == "munich" || v.city == "münchen" || v.city == "muenchen" } }
      .map { v =>
        {
          val latBucket = (v.loc.lat / 0.009).toInt
          val lonBucket = (v.loc.lon / 0.0134).toInt
          (f"$latBucket,$lonBucket", 1)
        }
      }
      .keyBy(0)
      .sum(1)
      .addSink(getKVProducer(Config.KafkaMunichHotspotsTopic))

    input
      .map { v => (Country.continent.getOrElse(v.country, Continent.Unknown), v.city, 1) }
      .filter { _._1 == Continent.Europe}
      .keyBy(0)
      .timeWindow(Time.days(1))
      .process(new TopKWindow(10))
      .addSink(getRankProducer(Config.KafkaTopCitiesTopic))

    env.execute("meetup pipeline")
  }

  def getConsumer(): FlinkKafkaConsumer[ObjectNode] = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.setProperty("group.id", Config.KafkaConsumerGroup)

    val consumer = new FlinkKafkaConsumer[ObjectNode](
      Config.KafkaMeetupEventsTopic,
      new JSONKeyValueDeserializationSchema(false),
      properties)

    consumer.setStartFromEarliest()

    consumer
  }

  def getKVProducer(topic: String): FlinkKafkaProducer[(String, Int)] = {
    new FlinkKafkaProducer[(String, Int)](topic, new AggregateSerializer(topic), getProducerProperties, Semantic.AT_LEAST_ONCE)
  }

  def getRankProducer(topic: String): FlinkKafkaProducer[(String, Int, String, Int)] = {
    new FlinkKafkaProducer[(String, Int, String, Int)](topic, new RankSerializer(topic), getProducerProperties, Semantic.AT_LEAST_ONCE)
  }

  def getProducerProperties(): Properties = {
    val properties = new Properties
    properties.put("bootstrap.servers", Config.KafkaURI)
    properties.put("transaction.max.timeout.ms", "900000")
    properties.put("transaction.timeout.ms", "600000")
    properties
  }
}
