package org.aiotrade.lib.avro

import scala.collection.generic.CanBuildFrom
import scala.collection.generic.GenericCompanion
import scala.collection.generic.GenericTraversableTemplate
import scala.collection.generic.SeqFactory
import scala.collection.mutable.Buffer
import scala.collection.mutable.BufferLike
import scala.collection.mutable.Builder
import scala.collection.mutable.IndexedSeqOptimized
import scala.collection.mutable.WrappedArray


/** @note copied from org.aiotrade.lib.collection to here to decouple dependency on it */

/** An implementation of the <code>Buffer</code> class using an array to
 *  represent the assembled sequence internally. Append, update and random
 *  access take constant time (amortized time). Prepends and removes are
 *  linear in the buffer size.
 *
 *  serialver -classpath ~/myapps/scala/lib/scala-library.jar:./ org.aiotrade.lib.collection.ArrayList
 *  
 *  @author  Matthias Zenger
 *  @author  Martin Odersky
 *  @author  Caoyuan Deng
 *  @version 2.8
 *  @since   1
 */
@serializable @SerialVersionUID(1529165946227428979L)
class ArrayList[@specialized A](override protected val initialSize: Int, protected val elementClass: Class[A] = null)(protected implicit val m: Manifest[A]
) extends Buffer[A]
     with GenericTraversableTemplate[A, ArrayList]
     with BufferLike[A, ArrayList[A]]
     with IndexedSeqOptimized[A, ArrayList[A]]
     with Builder[A, ArrayList[A]]
     with ResizableArray[A] {

  override def companion: GenericCompanion[ArrayList] = ArrayList

  def this()(implicit m: Manifest[A]) = this(16)

  def clear() { reduceToSize(0) }

  override def sizeHint(len: Int) {
    if (len > size && len >= 1) {
      val newarray = makeArray(len)
      Array.copy(array, 0, newarray, 0, size0)
      array = newarray
    }
  }
  
  /** Appends a single element to this buffer and returns
   *  the identity of the buffer. It takes constant time.
   *
   *  @param elem  the element to append.
   */
  def +=(elem: A): this.type = {
    ensureSize(size0 + 1)
    array(size0) = elem
    size0 += 1
    this
  }

  /** Appends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the itertable object.
   *  @return    the updated buffer.
   */
  override def ++=(xs: TraversableOnce[A]): this.type = {
    val len = xs match {
      case xs: IndexedSeq[_] => xs.length
      case _ => xs.size
    }
    ensureSize(size0 + len)
    xs match {
      /** @todo: https://lampsvn.epfl.ch/trac/scala/ticket/2564 */
      case xs: WrappedArray[_] =>
        Array.copy(xs.array, 0, array, size0, len)
        size0 += len
        this
      case xs: IndexedSeq[_] =>
        xs.copyToArray(array.asInstanceOf[scala.Array[Any]], size0, len)
        size0 += len
        this
      case _ =>
        super.++=(xs)
    }
  }
  
  /** Prepends a single element to this buffer and return
   *  the identity of the buffer. It takes time linear in 
   *  the buffer size.
   *
   *  @param elem  the element to append.
   *  @return      the updated buffer. 
   */
  def +=:(elem: A): this.type = {
    ensureSize(size0 + 1)
    copy(0, 1, size0)
    array(0) = elem
    size0 += 1
    this
  }
   
  /** Prepends a number of elements provided by an iterable object
   *  via its <code>iterator</code> method. The identity of the
   *  buffer is returned.
   *
   *  @param xs  the iterable object.
   *  @return    the updated buffer.
   */
  override def ++=:(xs: TraversableOnce[A]): this.type = { insertAll(0, xs.toTraversable); this }

  /** Inserts new elements at the index <code>n</code>. Opposed to method
   *  <code>update</code>, this method will not replace an element with a
   *  one. Instead, it will insert a new element at index <code>n</code>.
   *
   *  @param n     the index where a new element will be inserted.
   *  @param iter  the iterable object providing all elements to insert.
   *  @throws Predef.IndexOutOfBoundsException if <code>n</code> is out of bounds.
   */
  def insertAll(n: Int, seq: Traversable[A]) {
    if ((n < 0) || (n > size0)) throw new IndexOutOfBoundsException(n.toString)
    val len = seq match {
      case xs: IndexedSeq[_] => xs.length
      case _ => seq.size
    }
    ensureSize(size0 + len)
    copy(n, n + len, size0 - n)
    seq match {
      /** @todo: https://lampsvn.epfl.ch/trac/scala/ticket/2564 */
      case xs: WrappedArray[_] =>
        Array.copy(xs.array, 0, array, n, len)
      case _ =>
        seq.copyToArray(array.asInstanceOf[scala.Array[Any]], n)
    }
    size0 += len
  }
  
  /** Removes the element on a given index position. It takes time linear in
   *  the buffer size.
   *
   *  @param n  the index which refers to the first element to delete.
   *  @param count   the number of elemenets to delete
   *  @throws Predef.IndexOutOfBoundsException if <code>n</code> is out of bounds.
   */
  override def remove(n: Int, count: Int) {
    require(count >= 0, "removing negative number of elements")
    if (n < 0 || n > size0 - count) throw new IndexOutOfBoundsException(n.toString)
    copy(n + count, n, size0 - (n + count))
    reduceToSize(size0 - count)
  }

  /** Removes the element on a given index position
   *  
   *  @param n  the index which refers to the element to delete.
   *  @return  The element that was formerly at position `n`
   */
  def remove(n: Int): A = {
    val result = apply(n)
    remove(n, 1)
    result
  }

  /** Return a clone of this buffer.
   *
   *  @return an <code>ArrayList</code> with the same elements.
   */
  override def clone(): ArrayList[A] = new ArrayList[A](this.size) ++= this

  def result: ArrayList[A] = this

  /** Defines the prefix of the string representation.
   */
  override def stringPrefix: String = "ArrayList"

  /**
   * We need this toArray to export an array with the original type element instead of
   * scala.collection.TraversableOnce#toArray[B >: A : ClassManifest]: Array[B]:
   * def toArray[B >: A : ClassManifest]: Array[B] = {
   *   if (isTraversableAgain) {
   *     val result = new Array[B](size)
   *     copyToArray(result, 0)
   *     result
   *   }
   *   else toBuffer.toArray
   * }
   */
  def toArray: Array[A] = {
    val res = makeArray(length)
    Array.copy(array, 0, res, 0, length)
    res
  }
  
  override def copyToArray[B >: A](xs: Array[B], start: Int, len: Int) {
    Array.copy(array, start, xs, 0, len)
  }

  def sliceToArray(start: Int, len: Int): Array[A] = {
    val res = makeArray(len)
    Array.copy(array, start, res, 0, len)
    res
  }

  def sliceToArrayList(start: Int, len: Int): ArrayList[A] = {
    val res = new ArrayList(len)
    Array.copy(array, start, res.array, 0, len)
    res
  }

  // --- overrided methods for performance

  override def head: A = {
    if (isEmpty) throw new NoSuchElementException
    else apply(0)
  }

  override def last: A = {
    if (isEmpty) throw new NoSuchElementException
    else apply(size - 1)
  }

  override def reverse: ArrayList[A] = {
    val reversed = new ArrayList[A](this.size)
    var i = 0
    while (i < size) {
      reversed(i) = apply(size - 1 - i)
      i += 1
    }
    reversed
  }

  override def partition(p: A => Boolean): (ArrayList[A], ArrayList[A]) = {
    val l, r = new ArrayList[A]
    for (x <- this) (if (p(x)) l else r) += x
    (l, r)
  }
}

/** Factory object for <code>ArrayBuffer</code> class.
 *
 *  @author  Martin Odersky
 *  @version 2.8
 *  @since   2.8
 */
object ArrayList extends SeqFactory[ArrayList] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, ArrayList[A]] = new GenericCanBuildFrom[A]
  /**
   * we implement newBuilder for extending SeqFactory only. Since it's with no manifest arg,
   * we can only create a ArrayList[AnyRef] instead of ArrayList[A], but we'll define another
   * apply method to create ArrayList[A]
   */
  def newBuilder[A]: Builder[A, ArrayList[A]] = new ArrayList[AnyRef].asInstanceOf[ArrayList[A]]
  def apply[A: Manifest]() = new ArrayList[A]  
}


/** This class is used internally to implement data structures that
 *  are based on resizable arrays.
 *
 *  @tparam A    type of the elements contained in this resizeable array.
 *  
 *  @author  Matthias Zenger, Burak Emir
 *  @author Martin Odersky
 *  @version 2.8
 *  @since   1
 */
trait ResizableArray[@specialized A] extends IndexedSeq[A]
                                        with GenericTraversableTemplate[A, ResizableArray]
                                        with IndexedSeqOptimized[A, ResizableArray[A]] {

  protected implicit val m: Manifest[A]
  
  protected val elementClass: Class[A]
  
  override def companion: GenericCompanion[ResizableArray] = ResizableArray

  protected def initialSize: Int = 16
  protected[avro] var array: Array[A] = makeArray(initialSize)

  protected def makeArray(size: Int) = {
    if (elementClass != null) {
      java.lang.reflect.Array.newInstance(elementClass, size).asInstanceOf[Array[A]]
    } else {
      new Array[A](size) // this will return primitive element typed array if A is primitive, @see scala.reflect.Manifest
    }
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

  /** Fills the given array <code>xs</code> with at most `len` elements of
   *  this traversable starting at position `start`.
   *  Copying will stop once either the end of the current traversable is reached or
   *  `len` elements have been copied or the end of the array is reached.
   *
   *  @param  xs the array to fill.
   *  @param  start starting index.
   *  @param  len number of elements to copy
   */
  override def copyToArray[B >: A](xs: Array[B], start: Int, len: Int) {
    val len1 = len min (xs.length - start) min length
    Array.copy(array, 0, xs, start, len1)
  }

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
      if (!m.erasure.isPrimitive) {
        array(size0) = null.asInstanceOf[A]
      }
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


