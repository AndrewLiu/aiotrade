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
      Sec, SecDividend, SecInfo, SecIssue, SecStatus,
      Company, CompanyIndustry, Industry,
      Exchange, ExchangeCloseDate,
      Quote1d, Quote1m, MoneyFlow1d, MoneyFlow1m,
      Ticker, DealRecord
    ).dropCreate.messages.foreach(msg => println(msg.body))
  }

  def save(i: Int) {
    val i = new Industry
    i.code = "0001"
    Industry.save(i)

    val com = new Company
    com.listDate = System.currentTimeMillis
    com.shortName = "abc"
    Company.save(com)

    val inds = new CompanyIndustry
    inds.company = com
    inds.industry = i
    CompanyIndustry.save(inds)

    println("company's listDate: " + com.listDate)
    //println("company's industries: " + (c.industries.getValue map (_.industry) mkString(", ")))

    val info = new SecInfo
    info.symbol = "000001"
    info.name = "???A"
    SecInfo.save(info)

    val sec = new Sec
    sec.company = com
    sec.secInfo = info
    Sec.save_!(sec)

    com.sec = sec
    info.sec = sec
    Company.update(com)
    SecInfo.update(info)

    val cal = Calendar.getInstance
    val quote1d = new Quote
    //quote1d.id := 10000L
    quote1d.sec = sec
    quote1d.time = cal.getTimeInMillis
    quote1d.open = 1
    Quote1d.save(quote1d)

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val quote1da = new Quote
    quote1da.sec = sec
    quote1da.time = cal.getTimeInMillis
    quote1da.open = 1
    Quote1d.save(quote1da)

    val quote1m = new Quote
    quote1m.sec = sec
    quote1m.open = 1
    quote1m.time = System.currentTimeMillis
    Quote1m.save(quote1m)

    def makeTicker = {
      val ticker = new Ticker
      ticker.quote = quote1d
      ticker.time = System.currentTimeMillis
      val bidAskDepth = 10
      val bidAsks = new Array[Float](bidAskDepth * 4)
      ticker.bidAsks = bidAsks
      Ticker.save(ticker)
    }

    for (i <- 0 until 10) makeTicker
  }

  def select {
    val ticker1 = Ticker.get(1).get
    val decodedBidAsks = ticker1.bidAsks
    val depth = decodedBidAsks.length / 4
    println("Depth of bid ask: " + depth)
    
    val quote1d = Quote1d.get(1).get

    println("tickers of quote: " + (Quote1d.tickers(quote1d) map (x => x.time) mkString(", ")))

    val co = Company
    val ci = CompanyIndustry

    co.criteria.add(co.shortName like "a%").list foreach (c =>
      println(c.shortName)
    )


    val s1 = SELECT (co.*) FROM (co JOIN ci) WHERE (co.shortName LIKE "a") ORDER_BY (co.shortName ASC) list
    //val select1 = SELECT (co.*, cis.*) FROM (co JOIN cis) WHERE (co.shortName LIKE "a%") list

    s1 foreach println

    val com = Company.get(1).get
    println("com: " + com.shortName + ", com.sec: " + com.sec)
    println("com's industries: " + (Company.industries(com) map (_.industry)))
    
    val sec = Sec.get(1).get

    val quotes = Sec.dailyQuotes(sec)
    com.sec.dailyQuotes ++= quotes
    println("sec's Quote: " + com.sec.dailyQuotes)

    fetchAllSecs
  }

  private def fetchAllSecs {
    /* .prefetch(Sec.secInfo.asInstanceOf[Association[Any, Any]]) */
    val s = Sec
    val i = SecInfo
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
