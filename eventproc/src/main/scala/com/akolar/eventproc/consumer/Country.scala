package com.akolar.eventproc

import scala.collection.immutable.HashSet

object Country {
  val europe = HashSet[String](
    "AL", "AD", "AM", "AT", "BY", "BE", "BA", "BG", "CH", "CY", "CZ",
    "DE", "DK", "EE", "ES", "FO", "FI", "FR", "GB", "GE", "GI", "GR", "HU",
    "HR", "IE", "IS", "IT", "LT", "LU", "LV", "MC", "MK", "MT", "NO", "NL",
    "PO", "PT", "RO", "RU", "SE", "SI", "SK", "SM", "TR", "UA", "VA"
  )
  def inEurope(c: String): Boolean = europe.contains(c)
}
