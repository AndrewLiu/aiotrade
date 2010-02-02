/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.util.pool

import java.util.NoSuchElementException
import java.util.Stack

object StackObjectPool {
  /** The default cap on the number of "sleeping" instances in the pool. */
  val DEFAULT_MAX_SLEEPING = 8
  /**
   * The default initial size of the pool
   * (this specifies the size of the container, it does not
   * cause the pool to be pre-populated.)
   */
  val DEFAULT_INIT_SLEEPING_CAPACITY = 4
}
/**
 * A simple, {@link java.util.Stack Stack}-based {@link ObjectPool} implementation.
 * <p>
 * Given a {@link PoolableObjectFactory}, this class will maintain
 * a simple pool of instances.  A finite number of "sleeping"
 * or idle instances is enforced, but when the pool is
 * empty, new instances are created to support the new load.
 * Hence this class places no limit on the number of "active"
 * instances created by the pool, but is quite useful for
 * re-using <tt>Object</tt>s without introducing
 * artificial limits.
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 383290 $ $Date: 2006-03-05 02:00:15 -0500 (Sun, 05 Mar 2006) $
 */
/**
 * Create a new <tt>SimpleObjectPool</tt> using
 * the specified <i>factory</i> to create new instances,
 * capping the number of "sleeping" instances to <i>max</i>,
 * and initially allocating a container capable of containing
 * at least <i>init</i> instances.
 *
 * @param factory the {@link PoolableObjectFactory} used to populate the pool
 * @param maxIdle cap on the number of "sleeping" instances in the pool
 * @param initIdleCapacity initial size of the pool (this specifies the size of the container,
 *             it does not cause the pool to be pre-populated.)
 */
import StackObjectPool._ // @Note: put import here to get it visible in this(...) constructor
class StackObjectPool[T](factory: PoolableObjectFactory[T], maxIdle: Int, initIdleCapacity: Int) extends BaseObjectPool[T] {

  /** My pool. */
  protected var _pool = new Stack[T]

  /** My {@link PoolableObjectFactory}. */
  protected var _factory = factory

  /** The cap on the number of "sleeping" instances in the pool. */
  protected val _maxSleeping = if (maxIdle < 0) DEFAULT_MAX_SLEEPING else maxIdle

  /** Number of object borrowed but not yet returned to the pool. */
  protected var _numActive = 0

  val initcapacity = if (initIdleCapacity < 1) DEFAULT_INIT_SLEEPING_CAPACITY else initIdleCapacity

  _pool.ensureCapacity(if (initcapacity > _maxSleeping) _maxSleeping else initcapacity)

  /**
   * Create a new pool using
   * no factory. Clients must first populate the pool
   * using {@link #returnObject(java.lang.Object)}
   * before they can be {@link #borrowObject borrowed}.
   */
  def this() = {
    this(null, DEFAULT_MAX_SLEEPING, DEFAULT_INIT_SLEEPING_CAPACITY)
  }

  /**
   * Create a new pool using
   * no factory. Clients must first populate the pool
   * using {@link #returnObject(java.lang.Object)}
   * before they can be {@link #borrowObject borrowed}.
   *
   * @param maxIdle cap on the number of "sleeping" instances in the pool
   */
  def this(maxIdle: Int) = {
    this(null, maxIdle, DEFAULT_INIT_SLEEPING_CAPACITY)
  }

  /**
   * Create a new pool using
   * no factory. Clients must first populate the pool
   * using {@link #returnObject(java.lang.Object)}
   * before they can be {@link #borrowObject borrowed}.
   *
   * @param maxIdle cap on the number of "sleeping" instances in the pool
   * @param initIdleCapacity initial size of the pool (this specifies the size of the container,
   *             it does not cause the pool to be pre-populated.)
   */
  def this(maxIdle: Int, initIdleCapacity: Int) = {
    this(null, maxIdle, initIdleCapacity)
  }

  /**
   * Create a new <tt>StackObjectPool</tt> using
   * the specified <i>factory</i> to create new instances.
   *
   * @param factory the {@link PoolableObjectFactory} used to populate the pool
   */
  def this(factory: PoolableObjectFactory[T]) = {
    this(factory, DEFAULT_MAX_SLEEPING, DEFAULT_INIT_SLEEPING_CAPACITY)
  }

  /**
   * Create a new <tt>SimpleObjectPool</tt> using
   * the specified <i>factory</i> to create new instances,
   * capping the number of "sleeping" instances to <i>max</i>.
   *
   * @param factory the {@link PoolableObjectFactory} used to populate the pool
   * @param maxIdle cap on the number of "sleeping" instances in the pool
   */
  def this(factory: PoolableObjectFactory[T], maxIdle: Int) = {
    this(factory, maxIdle, DEFAULT_INIT_SLEEPING_CAPACITY)
  }

  @throws(classOf[RuntimeException])
  override def borrowObject :T  = synchronized {
    assertOpen
    var obj: Option[T] = None
    while (None == obj) {
      if (!_pool.empty) {
        obj = Some(_pool.pop)
      } else {
        if(null == _factory) {
          throw new NoSuchElementException
        } else {
          obj = Some(_factory.makeObject)
        }
      }
      if(null != _factory && None != obj) {
        _factory.activateObject(obj.get)
      }
      if (null != _factory && None != obj && !_factory.validateObject(obj.get)) {
        _factory.destroyObject(obj.get)
        obj = None
      }
    }
    _numActive += 1

    obj.get
  }

  @throws(classOf[RuntimeException])
  override def returnObject(obj: T): Unit = synchronized {
    assertOpen
    var obj1 = obj
    var success = true
    if (null != _factory) {
      if (!(_factory.validateObject(obj))) {
        success = false
      } else {
        try {
          _factory.passivateObject(obj)
        } catch {case e: Exception => success = false}
      }
    }

    var shouldDestroy = !success

    _numActive -= 1
    if (success) {
      var toBeDestroyed = null.asInstanceOf[T]
      if(_pool.size >= _maxSleeping) {
        shouldDestroy = true
        toBeDestroyed = _pool.remove(0) // remove the stalest object
      }
      _pool.push(obj)
      obj1 = toBeDestroyed // swap returned obj with the stalest one so it can be destroyed
    }
    notifyAll // _numActive has changed

    if(shouldDestroy) { // by constructor, shouldDestroy is false when _factory is null
      try {
        _factory.destroyObject(obj1)
      } catch {case e: Exception =>}
    }
  }

  @throws(classOf[RuntimeException])
  override def invalidateObject(obj: T): Unit = synchronized {
    assertOpen
    _numActive -= 1
    if (null != _factory ) {
      _factory.destroyObject(obj)
    }
    notifyAll // _numActive has changed
  }

  override def getNumIdle: Int = synchronized {
    assertOpen
    _pool.size
  }

  override def getNumActive: Int = synchronized {
    assertOpen
    _numActive
  }

  override def clear: Unit = synchronized {
    assertOpen
    if (null != _factory) {
      val it = _pool.iterator
      while(it.hasNext) {
        try {
          _factory.destroyObject(it.next)
        } catch {
          case e: Exception =>
            // ignore error, keep destroying the rest
        }
      }
    }
    _pool.clear
  }

  @throws(classOf[Exception])
  override def close: Unit = synchronized {
    clear
    _pool = null
    _factory = null
    super.close
  }

  /**
   * Create an object, and place it into the pool.
   * addObject() is useful for "pre-loading" a pool with idle objects.
   * @throws Exception when the {@link #_factory} has a problem creating an object.
   */
  @throws(classOf[RuntimeException])
  override def addObject: Unit = synchronized {
    assertOpen
    val obj = _factory.makeObject
    _numActive += 1   // A little slimy - must do this because returnObject decrements it.
    returnObject(obj)
  }

  @throws(classOf[IllegalStateException])
  override def setFactory(factory: PoolableObjectFactory[T]): Unit = synchronized {
    assertOpen
    if (0 < getNumActive) {
      throw new IllegalStateException("Objects are already active")
    } else {
      clear
      _factory = factory
    }
  }

}
