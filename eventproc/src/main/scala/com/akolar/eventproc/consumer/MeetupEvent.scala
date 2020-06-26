package com.akolar.eventproc.consumer

object MeetupEvent {
case class Event ( val country: String,
                       val city: String,
                       val loc: Location,
                       val ProcTime: Long,
                       val EventTime: Long )

case class Location ( val lat: Double,
                      val lon: Double )

}
