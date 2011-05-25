package org.aiotrade.lib.securities.model

import ru.circumflex.orm._

object Executions extends Table[Execution] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val price  = "price"  DOUBLE()
  val volume = "volume" DOUBLE()
  val amount = "amount" DOUBLE()

  val flag = "flag" TINYINT() // @Note jdbc type of TINYINT is Int

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  private val ONE_DAY = 24 * 60 * 60 * 1000

  def executionsOf(sec: Sec, dailyRoundedTime: Long): Seq[Execution] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
    ) ORDER_BY (this.time) list
  }
}

object Execution {
  // bit masks for flag
  val MaskEven          = 1 << 0   //    000...00000001
  val MaskIn            = 1 << 1   //    000...00000010
  val MaskOut           = 1 << 2   //    000...00000100
  val MaskSame          = 1 << 3   //    000...00001000
  val MaskUp            = 1 << 4   //    000...00010000
  val MaskDown          = 1 << 5   //    000...00100000
  private val flagbit3  = 1 << 6   //    000...01000000
  private val flagbit4  = 1 << 7   //    000...10000000
}

import Execution._
class Execution {
  @transient var _sec: Sec = _
  def sec = _sec
  def sec_=(sec: Sec) {
    _uniSymbol = sec.uniSymbol
    _sec = sec
  }
  
  private var _uniSymbol: String = _
  def uniSymbol = _uniSymbol
  
  var time: Long = -1

  var price:  Double = _
  var volume: Double = _
  var amount: Double = _

  var flag: Int = _ // @Note jdbc type of TINYINT is Int

  even_!
  same_!

  def even_? : Boolean = (flag & MaskEven) == MaskEven
  def in_?   : Boolean = (flag & MaskIn) == MaskIn
  def out_?  : Boolean = (flag & MaskOut) == MaskOut
  def even_! {flag = (((flag | MaskEven) & ~MaskIn) & ~MaskOut)}
  def out_!  {flag = (((flag | MaskOut) & ~MaskIn) & ~MaskEven)}
  def in_!   {flag = (((flag | MaskIn) & ~MaskOut) & ~MaskEven)}

  def same_? : Boolean = (flag & MaskSame) == MaskSame
  def up_?   : Boolean = (flag & MaskUp) == MaskUp
  def down_? : Boolean = (flag & MaskDown) == MaskDown
  def same_! {flag = (((flag | MaskSame) & ~MaskDown) & ~MaskUp)}
  def up_!   {flag = (((flag | MaskUp) & ~MaskDown) & ~MaskSame)}
  def down_! {flag = (((flag | MaskDown) & ~MaskUp) & ~MaskSame)}
}
