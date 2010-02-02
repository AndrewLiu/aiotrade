/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.actors

import scala.actors.Actor
import scala.collection.mutable.ListBuffer

trait ChainActor extends Actor {
  private val emptyAction: PartialFunction[Any, Unit] = {case _ =>}
  protected val actorActions = new ListBuffer[PartialFunction[Any, Unit]]

  def act {
    Actor.loop {
      react {
        chainReactions
      }
    }
  }

  private def chainReactions: PartialFunction[Any, Unit] = {
    val actions = actorActions.iterator
    var chained = if (actions.hasNext) actions.next else emptyAction
    while (actions.hasNext) {
      chained = chained orElse actions.next
    }
    chained
  }
}