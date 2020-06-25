package com.akolar.eventproc

object Config {
  val ZookeeperURI = sys.env.getOrElse("ZOOKEEPER_URI", "zookeeper:2181")
  val KafkaURI = sys.env.getOrElse("KAFKA_URI", "kafka:9092")
  val KafkaMeetupEventsTopic = sys.env.getOrElse("KAFKA_EVENTS_TOPIC", "meetup_events")
  val KafkaConsumerGroup = sys.env.getOrElse("KAFKA_EVENT_CONSUMER_GROUP", "cg_events")

  val ProducerIterations = math.max(1, sys.env.getOrElse("KAFKA_PRODUCER_ITERATIONS", "3").toInt)

  val InputFile = sys.env.getOrElse("INPUT_FILE", "/data/meetup.json")
}
