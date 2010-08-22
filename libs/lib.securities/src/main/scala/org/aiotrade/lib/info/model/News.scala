package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object Newses extends Table[News]{

  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val orgPublisher = "orgPublisher" VARCHAR(30) DEFAULT("''")
  val hotness = "hotness" FLOAT()
}

class News {

  var generalInfo : GeneralInfo = _

  var author : String = ""
  var orgPublisher : String = ""
  var hotness : Float = _
}
