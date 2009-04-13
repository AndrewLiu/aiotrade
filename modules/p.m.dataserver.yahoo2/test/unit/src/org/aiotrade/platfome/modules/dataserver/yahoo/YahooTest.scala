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
import org.aiotrade.lib.indicator.VOLIndicator
import org.aiotrade.lib.math.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.computable.Indicator
import org.aiotrade.lib.math.timeseries.datasource._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.dataserver._
import org.aiotrade.platform.modules.dataserver.yahoo._
import org.aiotrade.platform.modules.indicator.basic.{MAIndicator,RSIIndicator}

class YahooTest {

    @Before
    def setUp: Unit = {
    }

    @After
    def tearDown: Unit = {
    }

    @Test{val timeout=30000}
    def example = {
        new TestHelper().main("600373.SS")
    }

}

class TestHelper {

    def main(symbol:String) {
        val quoteServer  = classOf[YahooQuoteServer]
        val tickerServer = classOf[YahooTickerServer]

        val oneMinFreq = Frequency.ONE_MIN
        val dailyFreq = Frequency.DAILY

        val dailyQuoteContract = createQuoteContract(symbol, "", "", dailyFreq, false, quoteServer)

        val supportOneMin = dailyQuoteContract.isFreqSupported(oneMinFreq)

        val oneMinQuoteContract = createQuoteContract(symbol, "", "", oneMinFreq, false, quoteServer)
        val tickerContract = createTickerContract(symbol, "", "", oneMinFreq, tickerServer)

        val quoteContracts = List(dailyQuoteContract, oneMinQuoteContract)

        val sec = new Stock(symbol, quoteContracts, tickerContract)
        val market = YahooQuoteServer.marketOf(symbol)
        sec.market = market

        val dailyContents = createAnalysisContents(symbol, dailyFreq)
        dailyContents.addDescriptor(dailyQuoteContract)
        dailyContents.serProvider = sec
        loadSer(dailyContents)

        val rtContents = createAnalysisContents(symbol, oneMinFreq)
        rtContents.addDescriptor(oneMinQuoteContract)
        rtContents.serProvider = sec
        loadSer(rtContents)

        sec.subscribeTickerServer

        // wait for some seconds
        val t0 = System.currentTimeMillis
        var t1 = t0
        while (t1 - t0 < 10000) {t1 = System.currentTimeMillis}

        sec.serOf(dailyFreq).foreach{x => println("size of daily quote: " + x.size)}
        computeIndicators(dailyContents, sec.serOf(dailyFreq).get)
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

        contents.addDescriptor(createIndicatorDescriptor(classOf[MAIndicator], freq))
        //contents.addDescriptor(createIndicatorDescriptor(classOf[VOLIndicator], freq))
        contents.addDescriptor(createIndicatorDescriptor(classOf[RSIIndicator], freq))
        
        contents
    }

    private def createIndicatorDescriptor[T <: Indicator](clazz:Class[T], freq:Frequency) :IndicatorDescriptor = {
        val descriptor = new IndicatorDescriptor
        descriptor.active = true
        descriptor.serviceClassName = clazz.getName
        descriptor.freq = freq
        descriptor
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

    def computeIndicators(contents:AnalysisContents, masterSer:MasterSer) :Unit = {
        for (descriptor <- contents.lookupDescriptors(classOf[IndicatorDescriptor])) {
            if (descriptor.active && descriptor.freq.equals(masterSer.freq)) {
                descriptor.serviceInstance(Seq(masterSer)) match {
                    case None => println("Can not init instance of: " + descriptor.serviceClassName)
                    case Some(indicator) =>
                        /**
                         * @NOTICE
                         * As the quoteSer may has been loaded, there may be no more UpdatedEvent
                         * etc. fired, so, computeFrom(0) first.
                         */
                        indicator.computeFrom(0)
                }
            }
        }
    }
}
