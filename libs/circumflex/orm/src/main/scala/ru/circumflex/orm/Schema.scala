package ru.circumflex.orm

import ORM._

/**
 * Base functionality for SQL schema.
 */
class Schema(var schemaName: String) extends SchemaObject {

  def sqlCreate = dialect.createSchema(this)
  def sqlDrop = dialect.dropSchema(this)

  def objectName = schemaName

  override def equals(obj: Any) = obj match {
    case sc: Schema => sc.schemaName.equalsIgnoreCase(this.schemaName)
    case _ => false
  }

  override def hashCode = this.schemaName.toLowerCase.hashCode
}


object Schema {
  /**
   * Default public schema singleton.
   */
  object Default extends Schema(defaultSchemaName)

}

