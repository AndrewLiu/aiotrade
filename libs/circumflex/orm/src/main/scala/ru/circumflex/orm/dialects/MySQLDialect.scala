/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.circumflex.orm.dialects

import ru.circumflex.orm.Column
import ru.circumflex.orm.Dialect
import ru.circumflex.orm.Relation

class MySQLDialect extends Dialect {
  override def stringType = "varchar(4096)"
  override def timestampType = "timestamp"
  override def supportsSchema_?() = false
  override def supportDropConstraints_?() = false
  override def qualifyRelation(rel: Relation[_]) = rel.relationName
  override def autoIncrementExpression(col: Column[_, _]) = "auto_increment"
  override def prepareAutoIncrementColumn(col: Column[_, _]) = {}
  override def lastIdExpression(rel: Relation[_]) = "last_insert_id()"
}
