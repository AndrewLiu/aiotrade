package org.aiotrade.lib.model.securities

import ru.circumflex.orm.Table

object CompanyIndustry extends Table[CompanyIndustry] {
  val company  = "company_id"  REFERENCES(Company) // manyToOne(CompanyIndustry.company)
  val industry = "industry_id" REFERENCES(Industry) //manyToOne(CompanyIndustry.industry)
}

class CompanyIndustry {
  var company: Company  = _
  var industry: Industry = _
}
