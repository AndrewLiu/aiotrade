/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: BufferedIterator.scala 18799 2009-09-26 15:19:40Z stepancheg $


package org.aiotrade.lib.collection

/** Buffered iterators are iterators which provide a method <code>head</code>
 *  that inspects the next element without discarding it.
 *
 *  @author  Martin Odersky
 *  @version 2.8
 *  @since   2.8
 */
trait BufferedIterator[+A] extends Iterator[A] {

  /** Returns next element of iterator without advancing beyond it.
   */
  def head: A

  override def buffered: this.type = this
}
