package org.aiotrade.lib.info.model

import org.aiotrade.lib.securities.model.Flag
import ru.circumflex.orm._
import org.aiotrade.lib.math.timeseries.TVal

object Infos1d extends Infos
object Infos1m extends Infos

abstract class Infos extends Table[Info] {

}

class Info extends TVal with Flag {
  private var _infos: List[InfoContent] = List()

  def infos = _infos

  def += [T <: InfoContent](value: T){
    assert(value.publishTime == this.time,
             value + " is appended to a different TVal with time=" + this.time)

    _infos = _infos :+ value
  }

  def ++= [T <: InfoContent](values: List[T]){
    values.foreach(v => assert(v.publishTime == this.time,
             v + " is appended to a different TVal with time=" + this.time))

    _infos = _infos ++ values
  }
}
