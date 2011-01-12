package org.aiotrade.lib

package object amqp {
  /**
   * Use this Seq, we do not need to care abort the mutable/immutable one
   */
  type MessageSeq[A] = scala.collection.Seq[A]

  /**
   * Use this Set, we do not need to care abort the mutable/immutable one
   */
  type MessageSet[A] = scala.collection.Set[A]

  /**
   * Use this Map, we do not need to care abort the mutable/immutable one
   */
  type MessagMap[A, B] = scala.collection.Map[A, B]
}
