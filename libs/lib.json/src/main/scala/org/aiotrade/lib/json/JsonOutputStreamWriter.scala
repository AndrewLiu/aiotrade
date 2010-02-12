package org.aiotrade.lib.json

import java.io.OutputStream
import java.io.OutputStreamWriter

class JsonOutputStreamWriter(out: OutputStream, charsetName: String) extends OutputStreamWriter(out, charsetName) {
  def write(x: JsonSerializable) {
    write('{')

    write('"')
    write(x.getClass.getName)
    write('"')

    write(':')

    write('{')
    x.writeJson(this)
    write('}')

    write('}')
  }

  def write(xs: Array[_]) {
    write('[')
    var i = 0
    while (i < xs.length) {
      val x = xs(i)
      internalWrite(x)
      i += 1
      if (i < xs.length) {
        write(',')
      }
    }
    write(']')
  }

  def write(xs: List[_]) {
    write('[')
    var is = xs
    while (!is.isEmpty) {
      val x = is.head
      internalWrite(x)
      is = is.tail
      if (!is.isEmpty) {
        write(',')
      }
    }
    write(']')
  }

  /**
   * write field or pair
   */
  def write(name: String, value: Any) {
    write('"')
    write(name)
    write('"')

    write(':')

    internalWrite(value)
  }

  private def internalWrite(value: Any) {
    value match {
      case x: String =>
        write('"')
        write(x)
        write('"')
      case x: Int => write(x.toString)
      case x: Long => write(x.toString)
      case x: Float => write(x.toString)
      case x: Double => write(x.toString)
      case x: Boolean => write(x.toString)
      case x: JsonSerializable => write(x)
      case x: List[_] => write(x)
      case x: Array[_] => write(x)
      case _ => throw new UnsupportedOperationException(value + " cannot be json serialized")
    }
  }

}