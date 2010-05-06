package org.aiotrade.lib.model.securities

import java.util.Calendar
import ru.circumflex.orm.Criteria
import ru.circumflex.orm.DDLExport
import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.ORM._
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

/**
 * mysqldump5 -u root --no-data --database inca > nyapc.mysql
 */
object Model {
  
  object Category extends Table[Category] with LongIdPK[Category] {
    val name = stringColumn("name")     // creates a column
    .notNull                            // creates NOT NULL constraint
    .unique                             // creates UNIQUE constraint
    .validateNotEmpty                   // adds NotEmpty validation
    .validatePattern("^[a-zA-Z]{1,8}$") // adds Pattern validation
  }

  class Category extends Record[Category](Category) {
    val id = field(Category.id)
    val name = field(Category.name)
    val books = oneToMany(Book.category)    // allows navigating between associations transparently
  }

  class Book extends Record[Book](Book) {
    val id = field(Book.id)
    val title = field(Book.title)
    val category = manyToOne(Book.category)
  }

  object Book extends Table[Book] with LongIdPK[Book] {
    val title = stringColumn("title")
    .notNull
    .validateNotEmpty

    val category = longColumn("category_id")
    .references(Category)     // creates an association with Category
    .onDeleteSetNull          // specifies a foreign-key action
    .onUpdateCascade
  }

  def main(args: Array[String]) {
    new DDLExport(
      Company, CompanyIndustry, Industry,
      Sec, SecDividend, SecInfo, SecIssue, SecStatus,
      Exchange, ExchangeCloseDate,
      Quote1d, Quote1m, MoneyFlow1d, MoneyFlow1m,
      Ticker, DealRecord
    ).dropCreate

    val i = new Industry
    i.code := "0001"
    i.save
    val c = new Company
    c.listDate := System.currentTimeMillis
    c.shortName := "abc"
    c.save
    val ci = new CompanyIndustry
    ci.company := c
    ci.industry := i
    ci.save
    c.shortName
    println("company's listDate: " + c.listDate.get.getOrElse(0))
    println("company's industries: " + (c.industries.get map (_.industry) mkString(", ")))

    val sec = new Sec
    sec.company := c
    sec.save
    c.sec := sec
    c.update
    println("sec's current company: " + sec.company.get.get.shortName.get.get)
    println("sec's company history: " + (sec.companyHists.get map (_.shortName) mkString(", ")))
    
    val cal = Calendar.getInstance
    val quote1d = new Quote(Quote1d)
    //quote1d.id := 10000L
    quote1d.sec := sec
    quote1d.time := cal.getTimeInMillis
    quote1d.open := 1
    quote1d.save

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val quote1da = new Quote(Quote1d)
    quote1da.sec := sec
    quote1da.time := cal.getTimeInMillis
    quote1da.open := 1
    quote1da.save

    val quote1m = new Quote(Quote1m)
    quote1m.sec := sec
    quote1m.open := 1
    quote1m.time := System.currentTimeMillis
    quote1m.save

    def makeTicker = {
      val ticker = new Ticker
      ticker.quote := quote1d
      ticker.time := System.currentTimeMillis
      val bidAskDepth = 10
      val bidAsks = new Array[Float](bidAskDepth * 4)
      ticker.setBidAsks(bidAsks)
      ticker.save
    }

    for (i <- 0 until 10) makeTicker

    val ticker1 = Ticker.get(1).get
    val decodedBidAsks = ticker1.getBidAsks
    val depth = decodedBidAsks.length / 4
    println("Depth of bid ask: " + depth)

    println("tickers of quote: " + (quote1d.tickers.get map (x => x.time.get.get) mkString(", ")))

    // SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)
    // SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay = 2) AND intraDay = 2

//    Company.criteria.add("shortName" like "a%").list foreach (c =>
//      println(c.shortName)
//
//    )
//
//    select().from(Company as "c" join (CompanyIndustry as "b"), CompanyIndustry as "c1")
//    .where("c.industries" like "a%")
//    .orderBy(asc("c.name"))
//    .list

    c.shortName.get.getOrElse("")

    c.industries.get foreach {x => println(x.industry.get.getOrElse(new Industry).code.get.getOrElse("").getClass.getName)}
    //c.industries := List(ci)

    //new DDLExport(Category, Book).create   // creates database schema

  }

  def sometest {
    val company = Company.get(0).get.industries.get foreach {x => x.industry}
    val secinfo = SecInfo.get(0)
    // find category by id
    val c = Category.get(2l)
    // find all books
    val allBooks = Book.all
    // find books for category
    val cBooks = c.get.books
    // find books by title
    // cause: scala.tools.nsc.symtab.Symbols$CyclicReference: illegal cyclic reference involving class Criteria
    //Book.criteria.add("title" like "a%").list
    select().from(Book as "b").where("title" like "a%").list

    select().from(Category as "c" join (Book as "b"), Category as "c1")
    .where("c1.name" like "a%")
    .orderBy(asc("c.name"))
    .list

    select(count("b.id"), "c.name").from(Category as "c" join (Book as "b")).list
  }
}
