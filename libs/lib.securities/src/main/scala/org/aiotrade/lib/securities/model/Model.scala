package org.aiotrade.lib.securities.model

import java.util.Calendar
import ru.circumflex.orm._
import scala.collection.mutable.HashMap

/**
 * mysqldump5 -u root --no-data --database inca > inca.mysql
 *
 *  SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)
 *  SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay = 2) AND intraDay = 2
 */
object Model {
  val secs = new HashMap[String, Sec]
  
  def main(args: Array[String]) {
    schema
    (0 until 5) foreach save
    select
  }

  def schema = {
    new DDLUnit(
      Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Companies, CompanyIndustries, Industries,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, DealRecords
    ).dropCreate.messages.foreach(msg => println(msg.body))
  }

  def save(i: Int) {
    Exchange.allExchanges foreach println
    Exchange.allExchanges foreach Exchanges.save

    val i = new Industry
    i.code = "0001"
    Industries.save(i)

    val com = new Company
    com.listDate = System.currentTimeMillis
    com.shortName = "abc"
    Companies.save(com)

    val inds = new CompanyIndustry
    inds.company = com
    inds.industry = i
    CompanyIndustries.save(inds)

    println("company's listDate: " + com.listDate)

    val info = new SecInfo
    info.symbol = "000001"
    info.name = "???A"
    SecInfos.save(info)

    val sec = new Sec
    sec.company = com
    sec.secInfo = info
    sec.exchange = Exchange.SS
    Secs.save_!(sec)

    com.sec = sec
    info.sec = sec
    Companies.update(com)
    SecInfos.update(info)

    val cal = Calendar.getInstance
    val quote1d = new Quote
    //quote1d.id := 10000L
    quote1d.sec = sec
    quote1d.time = cal.getTimeInMillis
    quote1d.open = 1
    Quotes1d.save(quote1d)

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val quote1da = new Quote
    quote1da.sec = sec
    quote1da.time = cal.getTimeInMillis
    quote1da.open = 1
    Quotes1d.save(quote1da)

    val quote1m = new Quote
    quote1m.sec = sec
    quote1m.open = 1
    quote1m.time = System.currentTimeMillis
    Quotes1m.save(quote1m)

    def makeTicker = {
      val ticker = new Ticker
      ticker.quote = quote1d
      ticker.time = System.currentTimeMillis
      val bidAskDepth = 10
      val bidAsks = new Array[Float](bidAskDepth * 4)
      ticker.bidAsks = bidAsks
      Tickers.save(ticker)
    }

    for (i <- 0 until 10) makeTicker
  }

  def select {
    val ticker1 = Tickers.get(1).get
    val decodedBidAsks = ticker1.bidAsks
    val depth = decodedBidAsks.length / 4
    println("Depth of bid ask: " + depth)
    
    val quote1d = Quotes1d.get(1).get

    println("tickers of quote: " + (Quotes1d.tickers(quote1d) map (x => x.time) mkString(", ")))

    val co = Companies
    val ci = CompanyIndustries

    co.criteria.add(co.shortName like "a%").list foreach (c =>
      println(c.shortName)
    )


    val s1 = (SELECT (co.*, ci.*) FROM (co JOIN ci) WHERE (co.shortName LIKE "a%") ORDER_BY (co.shortName ASC) list)
    s1 foreach println

    val com = Companies.get(1).get
    Companies.sec(com) // fetch com.sec
    println("com: " + com.shortName + ", com.sec: " + com.sec)
    println("com's industries: " + (Companies.industries(com) map (CompanyIndustries.industry(_).getOrElse(null))))
    
    val sec = Secs.get(1).get

    val quotes = Secs.dailyQuotes(sec)
    println("sec's Quote: " + quotes)
    com.sec.dailyQuotes ++= quotes
    println("sec's Quote: " + com.sec.dailyQuotes)

    fetchAllSecs
  }

  private def fetchAllSecs {
    /* .prefetch(Sec.secInfo.asInstanceOf[Association[Any, Any]]) */
    val s = Secs
    val i = SecInfos
    (SELECT (s.*, i.*) FROM (s JOIN i) list) foreach {case (sec, info) =>
        if (info != null) {
          println("sec's info: " + sec.secInfo)
          println("secInfo's sec: " + info.sec)
          secs += (info.symbol -> sec)
        }
    }
//    Sec.all() foreach {sec =>
//      Sec.secInfo(sec) match {
//        case None =>
//        case Some(info) => secs += (info.symbol -> sec)
//      }
//    }
  }


}
