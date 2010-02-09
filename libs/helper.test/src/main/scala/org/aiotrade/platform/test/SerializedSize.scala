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
 Quote: 149 Bytes
 Ticker: 434 Bytes
 2000 symbols of tickers per internal: 847.65625K
 2000 symbols of tickers per day: -1711.966796875M
 Tickers one day per symbol: 1220.625K
 LightTicker: 187 Bytes
 2000 symbols of lighttickers per internal: 365.234375K
 2000 symbols of lighttickers per day: 1027.2216796875M
 Lighttickers one day per symbol: 525.9375K
 */
object SerializedSize {
  val internal = 5
  val secondsOneDay = 4 * 60 * 60
  val nTickersPerSymbol = secondsOneDay / 5


  def main(args: Array[String]) {
    val quote = new Quote // 149 bytes
    serialize(quote)
    
    val ticker = new Ticker // 368 bytes
    var tickerSize = serialize(ticker)
    println("2000 symbols of tickers per internal: " +  2000 * tickerSize / 1024.0 + "K")
    println("2000 symbols of tickers per day: " +  2000 * nTickersPerSymbol * tickerSize / 1024.0 / 1024.0 + "M")
    println("Tickers one day per symbol: " +  nTickersPerSymbol * tickerSize / 1024.0 + "K")

    tickerSize = serialize(ticker.toLightTicker)
    println("2000 symbols of lighttickers per internal: " +  2000 * tickerSize / 1024.0 + "K")
    println("2000 symbols of lighttickers per day: " +  2000 * nTickersPerSymbol * tickerSize / 1024.0 / 1024.0 + "M")
    println("Lighttickers one day per symbol: " +  nTickersPerSymbol * tickerSize / 1024.0 + "K")

  }

  def serialize(content: AnyRef) = {
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(content)
    store.close

    val body = bytes.toByteArray
    val size = body.length
    println(content.getClass.getSimpleName + ": " + size + " Bytes")
    size
  }
}

