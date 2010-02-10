package org.aiotrade.lib.io

import java.io.Reader
import scala.annotation.tailrec

/**
 * An incremental streaming reader, will not keep old data when refill
 *
 * @param data // input buffer with text in it that ready to read
 * @author Caoyuan Deng
 */
class RestReader(var data: Array[Char],  var pos: Int, var end: Int, var in: Reader) {

  val EOF = 0xFFFF.asInstanceOf[Char]

  var eof: Boolean = false     // true if the end of the stream was reached.
  var gPos: Long = 0           // global position = gPos + start

  def this(data: Array[Char], pos: Int, end: Int) = this(data, pos, end, null)
  def this(data: String, pos: Int, end: Int) = this(data.toCharArray, pos, end, null)
  def this(data: String) = this(data.toCharArray, 0, data.length)


  def this(in: Reader, buffer: Array[Char]) = this(buffer, 0, 0, in)
  // 8192 matches the default buffer size of a BufferedReader so double
  // buffering of the data is avoided.
  def this(in: Reader) = this(in, new Array[Char](8192))


  /**
   * Clean old data and read in more data, we do not keep old data in memory,
   * thus this reader is incremental streaming reader
   */
  private def fill {
    if (in != null) {
      gPos += end
      pos = 0
      val n = in.read(data, 0, data.length)
      end = if (n >= 0) n else 0
    }

    if (pos >= end) eof = true
  }

  def fillMore {
    fill
    if (eof) throw new Exception("null")
  }

  /** global pos the last char at */
  def count = gPos + pos - 1

  def next: Char = {
    if (pos >= end) fill

    if (pos >= end) {
      EOF
    } else {
      val c = data(pos)
      pos += 1
      c
    }
  }

  //@tailrec
  def nextSkipWs: Char = {
    next match {
      case ' ' | '\t' | '\n' | '\r' => nextSkipWs
      case c => c
    }
  }

  def charAt(i: Int): Char = data(i)

  /**
   * Char the last read one
   */
  def last: Char = {
    if (pos > 0) {
      data(pos - 1)
    } else throw new RuntimeException("Have not read yet")
  }
    
  def expect(chars: Array[Char]) {
    for (c <- chars) {
      val c1 = next
      if (c1 != c) {
        if (c1 == -1) {
          throw new RuntimeException("Unexpected EOF")
        } else {
          throw new RuntimeException("Expected " + new String(chars))
        }
      }
    }
  }

  /**
   * Sets the inputstream-pointer offset, measured from the beginning of this data,
   * at which the next read or write occurs.
   */
  def seek(pos: Int) {
    this.pos = pos
  }

  def skip(i: Int) {
    this.pos += 1
  }

  def backup(i: Int) {
    this.pos -= 1
  }

}
