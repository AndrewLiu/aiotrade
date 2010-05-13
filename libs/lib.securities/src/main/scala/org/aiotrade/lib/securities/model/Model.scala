package org.aiotrade.lib.securities.model

import java.util.Calendar
import ru.circumflex.orm.Criteria
import ru.circumflex.orm.DDLUnit
import ru.circumflex.orm.ORM._
import ru.circumflex.orm._

/**
 * mysqldump5 -u root --no-data --database inca > nyapc.mysql
 */
object Model {
  
  def main(args: Array[String]) {
    testSecurities
  }

  def testSecurities {
    new DDLUnit(
      Sec, SecDividend, SecInfo, SecIssue, SecStatus,
      Company, CompanyIndustry, Industry,
      Exchange, ExchangeCloseDate,
      Quote1d, Quote1m, MoneyFlow1d, MoneyFlow1m,
      Ticker, DealRecord
    ).dropCreate.messages.foreach(msg => println(msg.body))

    val i = new Industry
    i.code = "0001"
    Industry.save(i)


    val c = new Company
    c.listDate = System.currentTimeMillis
    c.shortName = "abc"
    Company.save(c)

    val inds = new CompanyIndustry
    inds.company = c
    inds.industry = i
    CompanyIndustry.save(inds)

    println("company's listDate: " + c.listDate)
    //println("company's industries: " + (c.industries.getValue map (_.industry) mkString(", ")))

    val sec = new Sec
    sec.company = c
    Sec.save_!(sec)

    c.sec = sec
    Company.save_!(c)
    //println("sec's current company: " + sec.company.getValue.shortName.getValue)
    //println("sec's company history: " + (sec.companyHists.getValue map (_.shortName.getValue) mkString(", ")))

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


    val ticker1 = Ticker.get(1).get
    val decodedBidAsks = ticker1.bidAsks
    val depth = decodedBidAsks.length / 4
    println("Depth of bid ask: " + depth)

    println("tickers of quote: " + (Quote1d.tickers(quote1d) map (x => x.time) mkString(", ")))

    // SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)
    // SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay = 2) AND intraDay = 2

    Company.criteria.add("shortName" like "a%").list foreach (c =>
      println(c.shortName)
    )

    val co = Company as "co"
    val ci = CompanyIndustry as "cis"

    val s1 = SELECT (co.*) FROM (co JOIN ci) WHERE ("co.shortName" LIKE "a") ORDER_BY ("co.shortName" ASC) list
    //val select1 = SELECT (co.*, cis.*) FROM (co JOIN cis) WHERE (co.shortName LIKE "a%") list

    s1 foreach println

    val com = Company.get(1).get
    println("com: " + com.shortName + ", com.sec: " + com.sec)
    println("com's industries: " + (Company.industries(com) map (_.industry)))
    
    
    val quotes = Sec.dailyQuotes(sec)
    com.sec.dailyQuotes ++= quotes
    println("sec's Quote: " + com.sec.dailyQuotes)

  }


}
