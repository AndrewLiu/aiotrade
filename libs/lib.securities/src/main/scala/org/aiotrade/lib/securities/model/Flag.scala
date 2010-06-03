/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

object Flag {
  // bit masks for flag
  val MaskClosed           = 1 << 0   // 1   2^^0    000...00000001
  val MaskVerified         = 1 << 1   // 2   2^^1    000...00000010
  private val flagbit3     = 1 << 2   // 4   2^^2    000...00000100
  private val flagbit4     = 1 << 3   // 8   2^^3    000...00001000
  private val flagbit5     = 1 << 4   // 16  2^^4    000...00010000
  private val flagbit6     = 1 << 5   // 32  2^^5    000...00100000
  private val flagbit7     = 1 << 6   // 64  2^^6    000...01000000
  private val MaskJustOpen = 1 << 7   // 128 2^^7    000...10000000
}

import Flag._
trait Flag {
  var flag: Int = 1 // dafault is closed

  def closed_? : Boolean = (flag & MaskClosed) == MaskClosed
  def closed_!   {flag |=  MaskClosed}
  def unclosed_! {flag &= ~MaskClosed}

  def justOpen_? : Boolean = (flag & MaskJustOpen) == MaskJustOpen
  def justOpen_!   {flag |=  MaskJustOpen}
  def unjustOpen_! {flag &= ~MaskJustOpen}
}
