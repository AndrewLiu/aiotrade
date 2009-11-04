/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.math.timeseries

final object Null {
  final val Byte    = java.lang.Byte      MIN_VALUE   // -128 ~ 127
  final val Short   = java.lang.Short     MIN_VALUE   // -32768 ~ 32767
  final val Char    = java.lang.Character MIN_VALUE   // 0(\u0000) ~ 65535(\uffff)
  final val Int     = java.lang.Integer   MIN_VALUE   // -2,147,483,648 ~ 2,147,483,647
  final val Long    = java.lang.Long      MIN_VALUE   // -9,223,372,036,854,775,808 ~ 9,223,372,036,854,775,807
  final val Float   = java.lang.Float     NaN
  final val Double  = java.lang.Double    NaN
  final val Boolean = false

  final def is(v: Byte)    = v == Byte
  final def is(v: Short)   = v == Short
  final def is(v: Char)    = v == Char
  final def is(v: Int)     = v == Int
  final def is(v: Long)    = v == Long
  final def is(v: Float)   = java.lang.Float. isNaN(v)
  final def is(v: Double)  = java.lang.Double.isNaN(v)
  final def is(v: Boolean) = v == Boolean

  final def not(v: Byte)    = v != Byte
  final def not(v: Short)   = v != Short
  final def not(v: Char)    = v != Char
  final def not(v: Int)     = v != Int
  final def not(v: Long)    = v != Long
  final def not(v: Float)   = !java.lang.Float. isNaN(v)
  final def not(v: Double)  = !java.lang.Double.isNaN(v)
  final def not(v: Boolean) = v != Boolean
}
