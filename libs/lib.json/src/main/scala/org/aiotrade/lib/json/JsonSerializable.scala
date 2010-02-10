package org.aiotrade.lib.json

import java.io.IOException
import java.io.Reader
import java.io.Writer

trait JsonSerializable {
  @throws(classOf[IOException])
  def writeJson(out: Writer)

  @throws(classOf[IOException])
  def readJson(in: Reader)
}
