/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object Industry extends Table[Industry] with LongIdPK[Industry] {
  val category = stringColumn("category")
  val code = stringColumn("code", 10)
  val name = stringColumn("name", 30)
}

class Industry extends Record[Industry](Industry) {
  val id = field(Industry.id)
  val category = field(Industry.category)
  val code = field(Industry.code)
  val name = field(Industry.name)
  val companies = oneToMany(CompanyIndustry.industry)
}
