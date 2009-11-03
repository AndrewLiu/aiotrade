/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: LinearSeq.scala 19354 2009-10-30 07:51:23Z rompf $


package org.aiotrade.lib.collection
package immutable

import generic._
import mutable.Builder

/** A subtrait of <code>collection.LinearSeq</code> which represents sequences
 *  that cannot be mutated.
 *
 *  @since 2.8
 */
trait LinearSeq[+A] extends Seq[A] 
                            with scala.collection.LinearSeq[A] 
                            with GenericTraversableTemplate[A, LinearSeq]
                            with LinearSeqLike[A, LinearSeq[A]] {
  override def companion: GenericCompanion[LinearSeq] = LinearSeq
}

/**
 * @since 2.8
 */
object LinearSeq extends SeqFactory[LinearSeq] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, LinearSeq[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, LinearSeq[A]] = new mutable.ListBuffer
}
