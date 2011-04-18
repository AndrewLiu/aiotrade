package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table

object CompanyIndustries extends Table[CompanyIndustry] {
  val company  = "companies_id" BIGINT() REFERENCES(Companies)
  val industry = "industries_id" BIGINT() REFERENCES(Industries)
  
  val companyIdx = getClass.getSimpleName + "_company_idx" INDEX(company.name)
  val industryIdx = getClass.getSimpleName + "_industry_idx" INDEX(industry.name)
}

class CompanyIndustry {
  var company: Company  = _
  var industry: Industry = _
}
