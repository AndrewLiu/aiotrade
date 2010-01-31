/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.amqp

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.ConnectionParameters
import scala.actors.Actor

/**
 * @Usage:
 * Instantiate the SampleTradeListener class and write a sample message generator
 * facade that sends trdae messages to the consumer TradeMessageGenerator.
 */
class SampleTradeListener {
  val params = new ConnectionParameters
  params.setUsername("guest")
  params.setPassword("guest")
  params.setVirtualHost("/")
  params.setRequestedHeartbeat(0)

  val factory = new ConnectionFactory(params)
  val amqp = new TradeDispatcher(factory, "localhost", 5672)
  amqp.start

  class TradeListener extends Actor {
    def act = {
      react {
        case msg@TradeMessage(contents: Trade) =>
          println("received trade: " + msg.message); act
      }
    }
  }
  
  val tradeListener = new TradeListener
  tradeListener.start
  amqp ! AddListener(tradeListener)
}

