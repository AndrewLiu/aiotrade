package org.aiotrade.lib.model.security

import ru.circumflex.orm.Criteria
import ru.circumflex.orm.DDLExport
import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.ORM._
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

/**
 * mysqldump5 -u root --no-data --database inca > nyapc.mysql
 */
object Schema {
  
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
      Market, MarketCloseDate,
      Quote1d, Quote1m, MoneyFlow1d, MoneyFlow1m,
      InnerDay, Ticker, BidAsk, DealRecord, MoneyFlowTicker
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
    println("listDate:" + c.listDate.get.getOrElse(0))
    println("listDate:" + c.listDate.get.getOrElse(0))
    c.industries.get foreach {x => println(x.industry)}

    val sec = new Sec
    sec.company := c
    sec.save
    val quote1d = new Quote(Quote1d)
    quote1d.open := 1
    quote1d.sec := sec
    quote1d.save

    val quote1m = new Quote(Quote1m)
    quote1m.open := 1
    quote1m.sec := sec
    quote1m.save

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
