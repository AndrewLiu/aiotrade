/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.math.timeseries

object Null {
  val Byte    = java.lang.Byte      MIN_VALUE   // -128 ~ 127
  val Short   = java.lang.Short     MIN_VALUE   // -32768 ~ 32767
  val Char    = java.lang.Character MIN_VALUE   // 0(\u0000) ~ 65535(\uffff)
  val Int     = java.lang.Integer   MIN_VALUE   // -2,147,483,648 ~ 2,147,483,647
  val Long    = java.lang.Long      MIN_VALUE   // -9,223,372,036,854,775,808 ~ 9,223,372,036,854,775,807
  val Float   = java.lang.Float     NaN
  val Double  = java.lang.Double    NaN
  val Boolean = false

  def is(v: Byte)    = v == Byte
  def is(v: Short)   = v == Short
  def is(v: Char)    = v == Char
  def is(v: Int)     = v == Int
  def is(v: Long)    = v == Long
  def is(v: Float)   = java.lang.Float. isNaN(v)
  def is(v: Double)  = java.lang.Double.isNaN(v)
  def is(v: Boolean) = v == Boolean

  def not(v: Byte)    = v != Byte
  def not(v: Short)   = v != Short
  def not(v: Char)    = v != Char
  def not(v: Int)     = v != Int
  def not(v: Long)    = v != Long
  def not(v: Float)   = !java.lang.Float. isNaN(v)
  def not(v: Double)  = !java.lang.Double.isNaN(v)
  def not(v: Boolean) = v != Boolean
}
