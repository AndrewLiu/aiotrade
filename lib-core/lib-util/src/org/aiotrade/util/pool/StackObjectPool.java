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
package org.aiotrade.util.pool;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

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
public class StackObjectPool<T> extends BaseObjectPool<T> {
    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed}.
     */
    public StackObjectPool() {
        this(null,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed}.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     */
    public StackObjectPool(int maxIdle) {
        this(null,maxIdle,DEFAULT_INIT_SLEEPING_CAPACITY);
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
    public StackObjectPool(int maxIdle, int initIdleCapacity) {
        this(null,maxIdle,initIdleCapacity);
    }

    /**
     * Create a new <tt>StackObjectPool</tt> using
     * the specified <i>factory</i> to create new instances.
     *
     * @param factory the {@link PoolableObjectFactory} used to populate the pool
     */
    public StackObjectPool(PoolableObjectFactory<T> factory) {
        this(factory,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new <tt>SimpleObjectPool</tt> using
     * the specified <i>factory</i> to create new instances,
     * capping the number of "sleeping" instances to <i>max</i>.
     *
     * @param factory the {@link PoolableObjectFactory} used to populate the pool
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     */
    public StackObjectPool(PoolableObjectFactory<T> factory, int maxIdle) {
        this(factory,maxIdle,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

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
    public StackObjectPool(PoolableObjectFactory<T> factory, int maxIdle, int initIdleCapacity) {
        _factory = factory;
        _maxSleeping = (maxIdle < 0 ? DEFAULT_MAX_SLEEPING : maxIdle);
        int initcapacity = (initIdleCapacity < 1 ? DEFAULT_INIT_SLEEPING_CAPACITY : initIdleCapacity);
        _pool = new Stack<T>();
        _pool.ensureCapacity( initcapacity > _maxSleeping ? _maxSleeping : initcapacity);
    }

    public synchronized T borrowObject() throws RuntimeException {
        assertOpen();
        T obj = null;
        while (null == obj) {
            if (!_pool.empty()) {
                obj = _pool.pop();
            } else {
                if(null == _factory) {
                    throw new NoSuchElementException();
                } else {
                    obj = _factory.makeObject();
                }
            }
            if(null != _factory && null != obj) {
                _factory.activateObject(obj);
            }
            if (null != _factory && null != obj && !_factory.validateObject(obj)) {
                _factory.destroyObject(obj);
                obj = null;
            }
        }
        _numActive++;
        return obj;
    }

    public synchronized void returnObject(T obj) throws RuntimeException {
        assertOpen();
        boolean success = true;
        if(null != _factory) {
            if(!(_factory.validateObject(obj))) {
                success = false;
            } else {
                try {
                    _factory.passivateObject(obj);
                } catch(Exception e) {
                    success = false;
                }
            }
        }

        boolean shouldDestroy = !success;

        _numActive--;
        if (success) {
            T toBeDestroyed = null;
            if(_pool.size() >= _maxSleeping) {
                shouldDestroy = true;
                toBeDestroyed = _pool.remove(0); // remove the stalest object
            }
            _pool.push(obj);
            obj = toBeDestroyed; // swap returned obj with the stalest one so it can be destroyed
        }
        notifyAll(); // _numActive has changed

        if(shouldDestroy) { // by constructor, shouldDestroy is false when _factory is null
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    public synchronized void invalidateObject(T obj) throws RuntimeException {
        assertOpen();
        _numActive--;
        if(null != _factory ) {
            _factory.destroyObject(obj);
        }
        notifyAll(); // _numActive has changed
    }

    public synchronized int getNumIdle() {
        assertOpen();
        return _pool.size();
    }

    public synchronized int getNumActive() {
        assertOpen();
        return _numActive;
    }

    public synchronized void clear() {
        assertOpen();
        if(null != _factory) {
            Iterator<T> it = _pool.iterator();
            while(it.hasNext()) {
                try {
                    _factory.destroyObject(it.next());
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
            }
        }
        _pool.clear();
    }

    public synchronized void close() throws Exception {
        clear();
        _pool = null;
        _factory = null;
        super.close();
    }

    /**
     * Create an object, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     * @throws Exception when the {@link #_factory} has a problem creating an object.
     */
    public synchronized void addObject() throws RuntimeException {
        assertOpen();
        T obj = _factory.makeObject();
        _numActive++;   // A little slimy - must do this because returnObject decrements it.
        this.returnObject(obj);
    }

    public synchronized void setFactory(PoolableObjectFactory<T> factory) throws IllegalStateException {
        assertOpen();
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    /** The default cap on the number of "sleeping" instances in the pool. */
    protected static final int DEFAULT_MAX_SLEEPING  = 8;

    /**
     * The default initial size of the pool
     * (this specifies the size of the container, it does not
     * cause the pool to be pre-populated.)
     */
    protected static final int DEFAULT_INIT_SLEEPING_CAPACITY = 4;

    /** My pool. */
    protected Stack<T> _pool = null;

    /** My {@link PoolableObjectFactory}. */
    protected PoolableObjectFactory<T> _factory = null;

    /** The cap on the number of "sleeping" instances in the pool. */
    protected int _maxSleeping = DEFAULT_MAX_SLEEPING;

    /** Number of object borrowed but not yet returned to the pool. */
    protected int _numActive = 0;
}
