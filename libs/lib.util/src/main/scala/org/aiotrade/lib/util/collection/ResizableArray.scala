/*                     __                                               *\
 **     ________ ___   / /  ___     Scala API                            **
 **    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
 **  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
 ** /____/\___/_/ |_/____/_/ | |                                         **
 **                          |/                                          **
 \*                                                                      */

// $Id: ResizableArray.scala 19219 2009-10-22 09:43:14Z moors $


package org.aiotrade.lib.util.collection


import scala.collection.generic.CanBuildFrom
import scala.collection.generic.GenericCompanion
import scala.collection.generic.GenericTraversableTemplate
import scala.collection.generic.SeqFactory
import scala.collection.mutable.Builder
import scala.collection.mutable.IndexedSeqLike

/** This class is used internally to implement data structures that
 *  are based on resizable arrays.
 *
 *  @author  Matthias Zenger, Burak Emir
 *  @author Martin Odersky
 *  @version 2.8
 *  @since   1
 */
abstract class ResizableArray[A](implicit m: Manifest[A]) extends IndexedSeq[A]
                                                             with GenericTraversableTemplate[A, ResizableArray]
                                                             with IndexedSeqLike[A, ResizableArray[A]] {

  override def companion: GenericCompanion[ResizableArray] = ResizableArray

  protected def initialSize: Int = 16
  protected var array: Array[A] = makeArray(initialSize)

  protected def makeArray(size: Int) = {
    val s = math.max(size, 1)
    (m.toString match {
        case "Byte"    => new Array[Byte](s)
        case "Short"   => new Array[Short](s)
        case "Char"    => new Array[Char](s)
        case "Int"     => new Array[Int](s)
        case "Long"    => new Array[Long](s)
        case "Float"   => new Array[Float](s)
        case "Double"  => new Array[Double](s)
        case "Boolean" => new Array[Boolean](s)
        case _ => new Array[A](s)
      }).asInstanceOf[Array[A]]
  }

  protected var size0: Int = 0

  //##########################################################################
  // implement/override methods of IndexedSeq[A]

  /** Returns the length of this resizable array.
   */
  def length: Int = size0

  def apply(idx: Int) = {
    if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    array(idx)
  }

  def update(idx: Int, elem: A) { 
    if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    array(idx) = elem
  }

  /** Fills the given array <code>xs</code> with the elements of
   *  this sequence starting at position <code>start</code>.
   *
   *  @param  xs the array to fill.
   *  @param  start starting index.
   */
  override def copyToArray[B >: A](xs: Array[B], start: Int) {
    Array.copy(array, 0, xs, start, size0)
  }

  /** Copy all elements to a buffer 
   *  @param   The buffer to which elements are copied
   override def copyToBuffer[B >: A](dest: Buffer[B]) {
   dest ++= (array: Seq[AnyRef]).asInstanceOf[Seq[B]]
   }
   */

  override def foreach[U](f: A =>  U) {
    var i = 0
    while (i < size) {
      f(array(i).asInstanceOf[A])
      i += 1
    }
  }

  //##########################################################################

  /** remove elements of this array at indices after <code>sz</code> 
   */
  def reduceToSize(sz: Int) {
    require(sz <= size0)
    while (size0 > sz) {
      size0 -= 1
      array(size0) = null.asInstanceOf[A]
    }
  }

  /** ensure that the internal array has at n cells */
  protected def ensureSize(n: Int) {
    if (n > array.length) {
      var newsize = array.length * 2
      while (n > newsize)
        newsize = newsize * 2
      val newar: Array[A] = makeArray(newsize)
      Array.copy(array, 0, newar, 0, size0)
      array = newar
    }
  }

  /** Swap two elements of this array.
   */
  protected def swap(a: Int, b: Int) {
    val h = array(a)
    array(a) = array(b)
    array(b) = h
  }

  /** Move parts of the array.
   */
  protected def copy(m: Int, n: Int, len: Int) {
    Array.copy(array, m, array, n, len)
  }
}

object ResizableArray extends SeqFactory[ResizableArray] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ResizableArray[A]] = new GenericCanBuildFrom[A]
  def newBuilder[A]: Builder[A, ResizableArray[A]] = new ArrayList[AnyRef].asInstanceOf[ArrayList[A]]
}
