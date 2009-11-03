/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: StringOps.scala 19334 2009-10-28 23:04:03Z odersky $


package org.aiotrade.lib.collection
package immutable

import mutable.StringBuilder

/**
 * @since 2.8
 */
class StringOps(override val repr: String) extends StringLike[String] {

  override protected[this] def thisCollection: WrappedString = new WrappedString(repr)
  override protected[this] def toCollection(repr: String): WrappedString = new WrappedString(repr)

  /** Creates a string builder buffer as builder for this class */
  override protected[this] def newBuilder = new StringBuilder

  override def toString = repr
}  
