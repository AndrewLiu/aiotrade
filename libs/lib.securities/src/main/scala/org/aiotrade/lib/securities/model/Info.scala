package org.aiotrade.lib.securities.model

import ru.circumflex.orm._
import org.aiotrade.lib.math.timeseries.TVal

object Infos1d extends Infos
object Infos1m extends Infos

abstract class Infos extends Table[Info] {

}

class Info extends TVal with Flag {
  var infos = List[String]()
}
