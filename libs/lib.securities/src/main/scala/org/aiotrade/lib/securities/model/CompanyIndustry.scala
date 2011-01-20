package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object CompanyIndustries extends Table[CompanyIndustry] {
  val company  = "companies_id".BIGINT REFERENCES(Companies)
  val industry = "industries_id".BIGINT REFERENCES(Industries)
}

class CompanyIndustry {
  var company: Company  = _
  var industry: Industry = _
}
