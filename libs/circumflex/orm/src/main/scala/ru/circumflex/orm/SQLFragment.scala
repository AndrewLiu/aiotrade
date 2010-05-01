package ru.circumflex.orm

import ORM._

trait SQLFragment extends SQLable {
  /**
   * Returns the parameters associated with this fragment.
   */
  def parameters: Seq[Any]

  /**
   * Renders this query by replacing parameter placeholders with actual values.
   */
  def toInlineSql: String = parameters.foldLeft(toSql)((sql, p) =>
    sql.replaceFirst("\\?", typeConverter.toString(p)))

}