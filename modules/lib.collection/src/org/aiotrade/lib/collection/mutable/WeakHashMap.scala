/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: WeakHashMap.scala 18801 2009-09-26 16:33:54Z stepancheg $


package org.aiotrade.lib.collection
package mutable

import JavaConversions._

/**
 * @since 2.8
 */
class WeakHashMap[A, B] extends JMapWrapper[A, B](new java.util.WeakHashMap) {
  override def empty = new WeakHashMap[A, B]
}
