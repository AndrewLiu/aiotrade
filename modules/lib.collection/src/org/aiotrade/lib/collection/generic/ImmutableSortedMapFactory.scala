/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: ImmutableSortedMapFactory.scala 18799 2009-09-26 15:19:40Z stepancheg $


package org.aiotrade.lib.collection
package generic

import mutable.{Builder, MapBuilder}

/** A template for companion objects of immutable.Map and subclasses thereof.
 *
 *  @since 2.8
 */
abstract class ImmutableSortedMapFactory[CC[A, B] <: immutable.SortedMap[A, B] with SortedMapLike[A, B, CC[A, B]]]
  extends SortedMapFactory[CC] {

  def newBuilder[A, B](implicit ord: Ordering[A]): Builder[(A, B), CC[A, B]] = 
    new MapBuilder[A, B, CC[A, B]](empty(ord))
}
