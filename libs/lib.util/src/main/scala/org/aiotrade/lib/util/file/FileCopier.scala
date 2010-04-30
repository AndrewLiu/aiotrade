/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.file

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileCopier {
  
  @throws(classOf[IOException])
  def copy(s: File, t: File) {
    copy(s, List(t))
  }

  @throws(classOf[IOException])
  def copy(s: File, ts: Seq[File]) {
    val in = new FileInputStream(s) getChannel
    
    for (t <- ts) {
      val out = new FileOutputStream(t) getChannel

      in.transferTo(0, s.length, out)
      out.close
    }
    
    in.close
  }
}