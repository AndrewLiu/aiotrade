package ru.circumflex.orm

import ORM._
import ru.circumflex.orm.Query.SQLQuery
import ru.circumflex.orm.RelationNode.ViewNode

/**
 * Represents an SQL view.
 */
abstract class View[R: Manifest] extends Relation[R] with SchemaObject {

  /**
   * Views are not updatable by default.
   */
  override def readOnly = true

  /**
   * Returns view's query.
   */
  def query: SQLQuery

  protected[orm] def virtualColumn[T](columnName: String): VirtualColumn[T, R] = {
    val col = new VirtualColumn[T, R](this, columnName)
    addColumns(col)
    return col
  }

  def as(alias: String) = new ViewNode(this).as(alias)

  def sqlDrop = dialect.dropView(this)

  def sqlCreate = dialect.createView(this)

  def objectName = qualifiedName
}

class VirtualColumn[T, R](view: View[R], name: String) extends Column[T, R] (view, name, "")
