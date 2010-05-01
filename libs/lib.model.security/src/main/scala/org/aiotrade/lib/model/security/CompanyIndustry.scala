/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.model.security

import ru.circumflex.orm.LongIdPK
import ru.circumflex.orm.Record
import ru.circumflex.orm.Table

object CompanyIndustry extends Table[CompanyIndustry] with LongIdPK[CompanyIndustry] {
  val company  = longColumn("company_id").references(Company)
  val industry = longColumn("industry_id").references(Industry)
}

class CompanyIndustry extends Record[CompanyIndustry](CompanyIndustry) {
  val company  = manyToOne(CompanyIndustry.company)
  val industry = manyToOne(CompanyIndustry.industry)
}
