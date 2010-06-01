/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

object Flag{
  // bit masks for flag
  val MaskClosed   = 0x01   // 1   2^^0    000...00000001
  val MaskVerified = 0x02   // 2   2^^1    000...00000010
  private val flagbit3     = 0x04   // 4   2^^2    000...00000100
  private val flagbit4     = 0x08   // 8   2^^3    000...00001000
  private val flagbit5     = 0x10   // 16  2^^4    000...00010000
  private val flagbit6     = 0x20   // 32  2^^5    000...00100000
  private val flagbit7     = 0x40   // 64  2^^6    000...01000000
  private val flagBit8     = 0x80   // 128 2^^7    000...10000000
}

import Flag._
trait Flag {
  var flag: Int = 1 // dafault is closed

  def closed_? = (flag & MaskClosed) == 1
  def closed_! {
    flag |= MaskClosed
  }
  def unclosed_! {
    flag &= ~MaskClosed
  }

}
