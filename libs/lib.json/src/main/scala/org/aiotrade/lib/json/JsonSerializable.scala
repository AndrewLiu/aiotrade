package org.aiotrade.lib.json

import java.io.IOException

trait JsonSerializable {
  @throws(classOf[IOException])
  def writeJson(out: JsonOutputStreamWriter)

  @throws(classOf[IOException])
  def readJson(fields: Map[String, _])
}
