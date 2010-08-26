package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.securities.InfoSer
import org.aiotrade.lib.securities.model.Sec

class InfoPointIndicator extends Indicator {
  sname = "INFO"
  lname = "INFO"
  isOverlapping = true

  private var infoSer: InfoSer = _

  val exists = TVar[Double]("I", Plot.Info)
  val infos = TVar[List[String]]("I", Plot.None)

  override def set(baseSer: BaseTSer) {
    // set baseSer to mfSer. @Note, this.freq is not set yet before super.set(mfSer)
    val sec = baseSer.serProvider.asInstanceOf[Sec]
    val freq = baseSer.freq
    val infoSer = sec.infoSerOf(freq).get
    if (!infoSer.isLoaded) {
      sec.loadInfoSerFromPersistence(infoSer)
    }
    this.infoSer = infoSer

    super.set(infoSer)
  }

  protected def computeCont(fromIdx: Int, size: Int) {
    var i = fromIdx
    while (i < size) {
      val info = infoSer.newses(i)
      if (info != null && !info.isEmpty) {
        exists(i) = 0
      }

      if (i % 5 == 0) {
        exists(i) = 0
      }
      i += 1
    }
  }
}
