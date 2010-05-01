package ru.circumflex.orm

/**
 * Type converters are used to read atomic values from JDBC
 * result sets and to set JDBC prepared statement values for execution.
 * If you intend to use custom types, provide your own implementation.
 */
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.Date
import ORM._

trait TypeConverter {

  def read(rs: ResultSet, alias: String): Option[Any] = {
    val result = rs.getObject(alias)
    if (rs.wasNull) return None
    else return Some(result)
  }

  def write(st: PreparedStatement, parameter: Any, paramIndex: Int): Unit = parameter match {
    case Some(p) => write(st, p, paramIndex)
    case None | null => st.setObject(paramIndex, null)
    case value => st.setObject(paramIndex, convert(value))
  }

  def convert(value: Any): Any = value match {
    case (p: Date) => new Timestamp(p.getTime)
    case value => value
  }

  def toString(value: Any): String = convert(value) match {
    case None | null => "null"
    case s: String => dialect.quoteLiteral(s)
    case d: Timestamp => dialect.quoteLiteral(d.toString)
    case other => other.toString
  }
}


object TypeConverter {
  object Default extends TypeConverter
}
