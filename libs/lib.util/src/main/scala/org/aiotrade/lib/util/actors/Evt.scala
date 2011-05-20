/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.util.actors

import scala.collection.mutable

/**
 * @TODO A sketch of business message protocals (APIs) design
 * 
 * @author Caoyuan Deng
 */
class Evt[T <: AnyRef](val tag: Int, val doc: String = "")(implicit m: Manifest[T]) {
  type tpe = T
  val tpeClass = m.erasure
  
  assert(!Evt.tagToEvt.contains(tag), "Tag: " + tag + " already existed!")
  Evt.tagToEvt(tag) = this
    
  /**
   * Avro schema: a Json String, We can implement a generic method by reflect 'tpe',
   * you can also override it to ge custom json
   */
  def schema: String = "" // todo
  
  def apply(evtValue: T) = (tag, evtValue)
  
  /** 
   * @Note Since  T is erasued after compiled, should check type of evt value via isInstance.
   * And, don't write as unapply(evt: (Int, T)), which will confuse the compiler to generate
   * wrong code for match {case .. case ..}
   */
  def unapply(evt: AnyRef): Option[T] = evt match {
    case (`tag`, value: T) if tpeClass.isInstance(value) => Some(value)
    case _ => None
  }
  
  override def toString = {
    "Evt(tag=" + tag + ", tpe=" + m.erasure + ", doc=\"" + doc + "\")"
  }
}

object Evt {
  private val tagToEvt = new mutable.HashMap[Int, Evt[_]]
  
  def evtTpeOf(tag: Int): Option[Class[_]] = tagToEvt.get(tag) map {_.tpeClass}
  def schemaOf(tag: Int): Option[String] = tagToEvt.get(tag) map {_.schema}
  
  // -- simple test
  def main(args: Array[String]) {
    object StrEvt extends Evt[String](-1)
    object IntEvt extends Evt[java.lang.Integer](-2)
    object SeqEvt extends Evt[(Int, String)](-3, "value, name")
    
    println("\n==== apis: ")
    println(StrEvt)
    println(IntEvt)
    println(SeqEvt)
    
    val a = StrEvt("a")
    val b = IntEvt(1)
    val c = SeqEvt(1, "a")
    val bada = (-1, 1)
    val badb = (-2, "a")
    
    val evts = List(a, b, c, bada, badb)
    
    println("\n==== evts: ")
    evts foreach println
    
    println("\n==== regular matching: ")
    evts foreach regularMatch
    
    println("\n==== advanced matching: ")
    evts foreach advancedMatch
    
    /** The regular match on those evts look like: */
    def regularMatch(v: Any) = v match {
      case (StrEvt.tag, astr: StrEvt.tpe) => println("Matched: " + v + " => " + astr) 
      case (IntEvt.tag, aint: IntEvt.tpe) => println("Matched: " + v + " => " + aint)
      case (SeqEvt.tag, aseq: SeqEvt.tpe) => println("Matched: " + v + " => " + aseq)
      case _ => println("Unmatched: " + v)
    }
    
    /** But we'd like a more concise approach: */
    def advancedMatch(v: Any) = v match {
      case StrEvt(astr) => println("Matched: " + v + " => " + astr) 
      case IntEvt(aint) => println("Matched: " + v + " => " + aint) 
      case SeqEvt(aint, astr) => println("Matched: " + v + " => (" + aint + ", " + astr + ")") 
      case _ => println("Unmatched: " + v)
    }
  }
}