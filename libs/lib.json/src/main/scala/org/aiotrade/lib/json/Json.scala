package org.aiotrade.lib.json

object Json {
  type Json = Either[Map[String, _], List[_]]
  type Object = Map[String, Map[String, _]]

  val TRUE_CHARS      = Array('t', 'r', 'u', 'e')
  val FALSE_CHARS     = Array('f', 'a', 'l', 's', 'e')
  val NULL_CHARS      = Array('n', 'u', 'l', 'l')
  val HEX_CHARS       = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  val VALUE_SEPARATOR = ','
  val NAME_SEPARATOR  = ':'
  val OBJECT_START    = '{'
  val OBJECT_END      = '}'
  val ARRAY_START     = '['
  val ARRAY_END       = ']'

  object Event {
    /** Event indicating a JSON string value, including member names of objects */
    val STRING = 1
    /** Event indicating a JSON number value which fits into a signed 64 bit integer */
    val LONG = 2
    /**
     * Event indicating a JSON number value which has a fractional part or an exponent
     * and with string length <= 23 chars not including sign.  This covers
     * all representations of normal values for Double.toString.
     */
    val NUMBER = 3
    /**
     * Event indicating a JSON number value that was not produced by toString of any
     * Java primitive numerics such as Double or Long.  It is either
     * an integer outside the range of a 64 bit signed integer, or a floating
     * point value with a string representation of more than 23 chars.
     */
    val BIGNUMBER = 4
    /** Event indicating a JSON boolean */
    val BOOLEAN = 5
    /** Event indicating a JSON null */
    val NULL = 6
    /** Event indicating the start of a JSON object */
    val OBJECT_START = 7
    /** Event indicating the end of a JSON object */
    val OBJECT_END = 8
    /** Event indicating the start of a JSON array */
    val ARRAY_START = 9
    /** Event indicating the end of a JSON array */
    val ARRAY_END = 10
    /** Event indicating the end of input has been reached */
    val EOF = 11
  }
}



