/*
 * Test.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.aiotrade.platfome.modules.dataserver.yahoo

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert._
import org.aiotrade.lib.math.timeseries.Frequency
import org.aiotrade.lib.math.timeseries.datasource._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.dataserver._
import org.aiotrade.platform.modules.dataserver.yahoo._

class YahooTest {

    @Before
    def setUp: Unit = {
    }

    @After
    def tearDown: Unit = {
    }

    @Test
    def example = {
        println(Frequency.DAILY)
        new TestHelper().main("600373.SS")
    }

}

class TestHelper {

    def main(symbol:String) {
        val quoteServer  = classOf[YahooQuoteServer]
        val tickerServer = classOf[YahooTickerServer]

        val freqOneMin = Frequency.ONE_MIN
        val freqDaily = Frequency.DAILY

        val dailyQuoteContract = createQuoteContract(symbol, "", "", freqDaily, false, quoteServer)

        val supportOneMin = dailyQuoteContract.isFreqSupported(freqOneMin)

        val oneMinQuoteContract = createQuoteContract(symbol, "", "", freqOneMin, false, quoteServer)
        val tickerContract = createTickerContract(symbol, "", "", freqOneMin, tickerServer)

        val quoteContracts = List(dailyQuoteContract, oneMinQuoteContract)

        val sec = new Stock(symbol, quoteContracts, tickerContract)
        val market = YahooQuoteServer.marketOf(symbol)
        sec.market = market

        val dailyContents = createAnalysisContents(symbol, freqDaily)
        dailyContents.addDescriptor(dailyQuoteContract)
        dailyContents.serProvider = sec
        loadSer(dailyContents)

        val rtContents = createAnalysisContents(symbol, freqOneMin)
        rtContents.addDescriptor(oneMinQuoteContract)
        rtContents.serProvider = sec
        loadSer(rtContents)

        sec.subscribeTickerServer
        sec.serOf(freqDaily).foreach{x => println(x.size)}
    }

    private def createQuoteContract(symbol:String, category:String , sname:String, freq:Frequency , refreshable:boolean, server:Class[_]) :QuoteContract = {
        val dataContract = new QuoteContract

        dataContract.active = true
        dataContract.serviceClassName = server.getName

        dataContract.symbol = symbol
        dataContract.category = category
        dataContract.shortName = sname
        dataContract.secType = Sec.Type.Stock
        dataContract.exchange = "SSH"
        dataContract.primaryExchange = "SSH"
        dataContract.currency = "USD"

        dataContract.dateFormatString = "yyyy-MM-dd-HH-mm"
        dataContract.freq = freq
        dataContract.refreshable = refreshable
        dataContract.refreshInterval = 5

        dataContract
    }

    private def createTickerContract(symbol:String, category:String, sname:String, freq:Frequency, server:Class[_]) :TickerContract = {
        val dataContract = new TickerContract

        dataContract.active = true
        dataContract.serviceClassName = server.getName

        dataContract.symbol = symbol
        dataContract.category = category
        dataContract.shortName = sname
        dataContract.secType = Sec.Type.Stock
        dataContract.exchange = "SSH"
        dataContract.primaryExchange = "SSH"
        dataContract.currency = "USD"

        dataContract.dateFormatString = "yyyy-MM-dd-HH-mm-ss"
        dataContract.freq = freq
        dataContract.refreshable = true
        dataContract.refreshInterval = 5

        dataContract
    }

    private def createAnalysisContents(symbol:String, freq:Frequency) :AnalysisContents = {
        val contents = new AnalysisContents(symbol)

        //        contents.addDescriptor(createIndicatorDescriptor(MAIndicator.class, freq));
        //        contents.addDescriptor(createIndicatorDescriptor(VOLIndicator.class, freq));
        //        contents.addDescriptor(createIndicatorDescriptor(RSIIndicator.class, freq));
        //
        contents
    }

    private def loadSer(contents:AnalysisContents) :Unit = {
        val quoteContract = contents.lookupActiveDescriptor(classOf[QuoteContract]) match {
            case None => return
            case Some(x) => x
        }

        val freq = quoteContract.freq
        if (!quoteContract.isFreqSupported(freq)) {
            return
        }

        val sec = contents.serProvider
        var mayNeedsReload = false
        if (sec == null) {
            return
        } else {
            mayNeedsReload = true
        }

        if (mayNeedsReload) {
            sec.clearSer(freq)
        }

        if (!sec.isSerLoaded(freq)) {
            sec.loadSer(freq)
        }
    }

}
