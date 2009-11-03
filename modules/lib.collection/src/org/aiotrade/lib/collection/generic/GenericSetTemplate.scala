/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: GenericSetTemplate.scala 18800 2009-09-26 15:37:19Z stepancheg $


package org.aiotrade.lib.collection
package generic

/**
 * @since 2.8
 */
trait GenericSetTemplate[A, +CC[X] <: Set[X]] extends GenericTraversableTemplate[A, CC] { 
  def empty: CC[A] = companion.empty[A]
}

