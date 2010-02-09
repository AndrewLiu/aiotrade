/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.ObjectOutputStream
import java.io.OutputStreamWriter
import java.util.Date
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.aiotrade.lib.securities.Quote
import org.aiotrade.lib.securities.LightTicker
import org.aiotrade.lib.securities.Ticker
import scala.util.Random

/**
 * As 8 bits in a byte mate. 1KB/s=8Kbit/s
 * According to test, the averagy band of 2M ADSL is only 200kb/s per user
 * 
 ======
 Quote: 149 Bytes
 ======
 Ticker: 445 Bytes
 2000 symbols of tickers per internal: 869.140625K
 2000 symbols of tickers per day: 2444.4580078125M
 Tickers one day per symbol: 1251.5625K
 ======
 LightTicker: 198 Bytes
 2000 symbols of lighttickers per internal: 386.71875K
 2000 symbols of lighttickers per day: 1087.646484375M
 Lighttickers one day per symbol: 556.875K
 ====== json =====
 LightTicker: 106 Bytes
 json string: {"L":{"s":"600001.SS","t":1265742281236,"v":[980.2116,61.16,51.15,31.13,41.14,114.41177,762.84644,21.12]}}
 Map(L -> Map(t -> 1265742281236, s -> 600001.SS, v -> List(980.2116, 61.16, 51.15, 31.13, 41.14, 114.41177, 762.84644, 21.12)))
 json object: 600001.SS, 03:04, [980.2116,61.16,51.15,31.13,41.14,114.41177,762.84644,21.12]
 litghticker json: 106 Bytes
 2000 symbols of lighttickers per internal: 207.03125K
 2000 symbols of lighttickers per day: 582.275390625M
 Lighttickers one day per symbol: 298.125K
 ==== json gzipped ====
 litghticker json: 101 Bytes
 2000 symbols of lighttickers per internal: 197.265625K
 2000 symbols of lighttickers per day: 554.8095703125M
 Lighttickers one day per symbol: 284.0625K
 */
object SerializedSize {
  val internal = 5
  val secondsOneDay = 4 * 60 * 60
  val nTickersPerSymbol = secondsOneDay / 5.0
  val nSymbols = 2000
  val kilo = 1024.0

  def main(args: Array[String]) {
    val now = new Date

    val quote = new Quote
    javaSerialize(quote)
    
    val ticker = new Ticker
    ticker.symbol = "600001.SS"
    ticker.time = now.getTime
    ticker.prevClose = Random.nextFloat * 1000
    ticker.dayAmount = Random.nextFloat * 1000
    ticker.dayVolume = Random.nextFloat * 1000
    ticker.dayChange = 21.12f
    ticker.dayHigh   = 31.13f
    ticker.dayLow    = 41.14f
    ticker.dayOpen   = 51.15f
    ticker.lastPrice = 61.16f
    var body = javaSerialize(ticker)
    var size = body.length
    println(nSymbols + " symbols of tickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of tickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Tickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

    body = javaSerialize(ticker.toLightTicker)
    size = body.length
    println(nSymbols + " symbols of lighttickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of lighttickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Lighttickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

    val lticker = ticker.toLightTicker
    body = jsonSerialize(lticker)
    size = body.length
    println("json string: " + new String(body))
    println("json object: " + jsonDeserialize(body))
    println("litghticker json: " + size + " Bytes")
    println(nSymbols + " symbols of lighttickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of lighttickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Lighttickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

    println("==== json gzipped ====")
    size = zipToBytes(body).length
    println("litghticker json: " + size + " Bytes")
    println(nSymbols + " symbols of lighttickers per internal: " + (nSymbols * size / kilo) + "K")
    println(nSymbols + " symbols of lighttickers per day: " + (nSymbols * nTickersPerSymbol * size / kilo / kilo) + "M")
    println("Lighttickers one day per symbol: " + (nTickersPerSymbol * size / kilo) + "K")

  }

  def javaSerialize(content: AnyRef): Array[Byte] = {
    val bytes = new ByteArrayOutputStream
    val store = new ObjectOutputStream(bytes)
    store.writeObject(content)
    store.close

    val body = bytes.toByteArray
    val size = body.length
    println("======")
    println(content.getClass.getSimpleName + ": " + size + " Bytes")
    body
  }

  def jsonSerialize(content: LightTicker): Array[Byte] = {
    val bytes = new ByteArrayOutputStream
    val store = new OutputStreamWriter(bytes, "UTF-8")
    content.writeJson(store)
    store.close

    val body = bytes.toByteArray
    val size = body.length
    println("====== json =====")
    println(content.getClass.getSimpleName + ": " + size + " Bytes")
    body
  }

  def jsonDeserialize(body: Array[Byte]): LightTicker = {
    val in = new InputStreamReader(new ByteArrayInputStream(body))
    val ticker = new LightTicker
    ticker.readJson(in)
    ticker
  }


  @throws(classOf[IOException])
  def zipToBytes(input: Array[Byte]): Array[Byte] = {
    val bytes = new ByteArrayOutputStream
    val store = new BufferedOutputStream(new GZIPOutputStream(bytes))
    store.write(input)
    //store.write(str.getBytes("UTF-8"))
    store.close

    val body= bytes.toByteArray
    bytes.close
    body
  }

  @throws(classOf[IOException])
  def unzipFromBytes(body: Array[Byte]): String = {
    val is = new ByteArrayInputStream(body)
    val bis = new BufferedInputStream(new GZIPInputStream(is))
    val store = new ByteArrayOutputStream
    val buf = new Array[Byte](1024)
    var len = -1
    while ({len = bis.read(buf); len > 0}) {
      store.write(buf, 0, len)
    }

    val str = store.toString("UTF-8")
    is.close
    bis.close
    store.close
    str
  }
}

