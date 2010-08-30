/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.sector.model

import ru.circumflex.orm.Table

object BullVSBears extends Table[BullVSBear]{
  val time = "time" BIGINT
  val ratio = "ratio" FLOAT()
  var summary  = "summary" VARCHAR(3000)
  INDEX(getClass.getSimpleName + "_time_idx", time.name)
}

class BullVSBear {
    var time : Long = _
    var ratio : Float = _
    var summary : String = ""
}
