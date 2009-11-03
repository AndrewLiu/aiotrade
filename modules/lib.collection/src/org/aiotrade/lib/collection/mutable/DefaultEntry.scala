/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id: DefaultEntry.scala 18801 2009-09-26 16:33:54Z stepancheg $


package org.aiotrade.lib.collection
package mutable

/**
 * @since 2.3
 */
@serializable
final class DefaultEntry[A, B](val key: A, var value: B) 
      extends HashEntry[A, DefaultEntry[A, B]]
