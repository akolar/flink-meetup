package com.akolar.eventproc.consumer

import java.lang
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.akolar.eventproc.consumer.Continent.Continent
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.immutable.ListMap

class TopKWindow(n: Int) extends ProcessWindowFunction[(Continent.Value, String, Int), (String, Int, String, Int), Tuple, TimeWindow] {
  override def process(key: Tuple,
                       context: Context,
                       elements: Iterable[(Continent.Value, String, Int)],
                       out: Collector[(String, Int, String, Int)]): Unit = {

    val byCity = collection.mutable.Map[String, Int]().withDefaultValue(0)
    for (el <- elements) {
      byCity(el._2) += el._3
    }

    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sorted = ListMap(byCity.toSeq.sortWith(_._2 > _._2):_*)
    sorted.take(this.n).zipWithIndex.foreach {
      case((city, count), rank) => out.collect((
        dateFormat.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(context.window.getStart), ZoneId.of("UTC"))),
        rank+1,
        city,
        count))
    }
  }
}
