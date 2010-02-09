/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import org.aiotrade.lib.securities.Quote
import org.aiotrade.lib.securities.Ticker

/**
 ======
 Quote: 149 Bytes
 ======
 Ticker: 434 Bytes
 2000 symbols of tickers per internal: 847.65625K
 2000 symbols of tickers per day: 2384.033203125M
 Tickers one day per symbol: 1220.625K
 ======
 LightTicker: 187 Bytes
 2000 symbols of lighttickers per internal: 365.234375K
 2000 symbols of lighttickers per day: 1027.2216796875M
 Lighttickers one day per symbol: 525.9375K
 */
object SerializedSize {
  val internal = 5
  val secondsOneDay = 4 * 60 * 60
  val nTickersPerSymbol = secondsOneDay / 5.0
  val nSymbols = 2000
  val kilo = 1024.0

  def main(args: Array[String]) {
    val quote = new Quote
    serialize(quote)
    
    val ticker = new Ticker
    var size = serialize(ticker)
    println(nSymbols + " symbols of tickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of tickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Tickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

    size = serialize(ticker.toLightTicker)
    println(nSymbols + " symbols of lighttickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of lighttickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Lighttickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

  }

  def serialize(content: AnyRef): Int = {
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(content)
    store.close

    val body = bytes.toByteArray
    val size = body.length
    println("======")
    println(content.getClass.getSimpleName + ": " + size + " Bytes")
    size
  }
}

