package org.aiotrade.lib.amqp

import com.rabbitmq.client.ConnectionFactory
import org.aiotrade.lib.util.pool.StackObjectPool
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.pool.PoolableObjectFactory

object RpcClientPool {
  def apply(factory: ConnectionFactory, exchange: String, maxIdle: Int, initIdleCapacity: Int) =
    new RpcClientPool(factory, exchange, maxIdle, initIdleCapacity)
}

class RpcClientPool(factory: ConnectionFactory, exchange: String, maxIdle: Int, initIdleCapacity: Int
) extends StackObjectPool[RpcClient](maxIdle, initIdleCapacity) with PoolableObjectFactory[RpcClient] {
  private val log = Logger.getLogger(this.getClass.getName)

  factory_=(this)

  @throws(classOf[RuntimeException])
  final def activate(obj: RpcClient) {}

  @throws(classOf[RuntimeException])
  final def destroy(obj: RpcClient) {}

  @throws(classOf[RuntimeException])
  final def create = {
    val client = new RpcClient(factory, exchange)
    try {
      client.connect
    } catch {
      case ex => log.log(Level.SEVERE, ex.getMessage, ex)
    }

    client
  }

  @throws(classOf[RuntimeException])
  final def passivate(obj: RpcClient) {}

  final def validate(obj: RpcClient) = obj.isConnected
}
