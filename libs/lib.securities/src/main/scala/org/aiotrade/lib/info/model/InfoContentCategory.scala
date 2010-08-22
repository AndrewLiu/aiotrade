package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object InfoContentCategories extends Table[InfoContentCategory]{
  val generalInfo =  "generalInfos_id" REFERENCES(GeneralInfos)
  val category = "categories_id" REFERENCES(ContentCategories)
  
}

class InfoContentCategory {
  var generalInfo : GeneralInfo= _
  var category : ContentCategory = _
}
