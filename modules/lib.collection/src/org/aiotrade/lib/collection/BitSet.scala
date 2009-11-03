/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: BitSet.scala 18799 2009-09-26 15:19:40Z stepancheg $


package org.aiotrade.lib.collection

import generic._

/** common base class for mutable and immutable bit sets
 *
 *  @since 1
 */
trait BitSet extends Set[Int] 
                with BitSetLike[BitSet] {
  override def empty: BitSet = BitSet.empty
}

/** A factory object for bitsets
 *
 *  @since 2.8
 */
object BitSet extends BitSetFactory[BitSet] {
  val empty: BitSet = immutable.BitSet.empty
}

