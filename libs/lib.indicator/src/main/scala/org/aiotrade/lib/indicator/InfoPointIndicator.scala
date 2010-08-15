package org.aiotrade.lib.indicator

class InfoPointIndicator extends Indicator {
  sname = "INFO"
  lname = "INFO"
  isOverlapping = true

  val vs = TVar[Double]("I", Plot.Info)
  val infos = TVar[List[String]]("I", Plot.None)

  protected def computeCont(fromIdx: Int, size: Int) {
    var i = fromIdx
    while (i < size) {
      if (i % 5 == 0) {
        vs(i) = 0
      }
      i += 1
    }
  }
}
