package org.aiotrade.lib.charting.chart

import org.aiotrade.lib.charting.widget.XDot
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import java.awt.Color
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.HeavyPathWidget

class InfoPointChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _

    def set(v: TVar[_]) {
      this.v = v
    }
  }

  type M = Model

  protected def createModel = new Model

  protected def plotChart {
    val m = model
    val color = Color.YELLOW
    setForeground(color)

    val heavyPathWidget = addChild(new HeavyPathWidget)

    val y = datumPlane.yChartUpper + 2

    val template = new XDot
    var bar = 1
    while (bar <= nBars) {

      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          val value = model.v.double(time)

          if (Null.not(value)) {
            template.model.set(xb(bar), y, wBar)
            template.setForeground(color)
            template.plot
            heavyPathWidget.appendFrom(template)
          }
        }

        i += 1
      }

      bar += nBarsCompressed
    }

  }


}
