package com.akolar.eventproc.consumer

object Continent extends Enumeration {
  type Continent = Value
  val Unknown = Value("n/a")
  val Europe = Value("Europe")
  val NorthAmerica = Value("NorthAmerica")
  val SouthAmerica = Value("SouthAmerica")
  val Antarctica = Value("Antarctica")
  val Asia = Value("Asia")
  val Africa = Value("Africa")
  val Australia = Value("Australia")
}
