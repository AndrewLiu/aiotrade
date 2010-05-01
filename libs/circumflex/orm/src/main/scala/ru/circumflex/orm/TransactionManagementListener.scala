package ru.circumflex.orm

/**
 * Ensures that current transaction is committed and that contextual connection is closed
 * at the end of request processing cycle.
 */
import javax.servlet.ServletRequestEvent
import javax.servlet.ServletRequestListener
import org.slf4j.LoggerFactory
import ORM._

class TransactionManagementListener extends ServletRequestListener {
  protected val log = LoggerFactory.getLogger("ru.circumflex.orm")

  def requestInitialized(sre: ServletRequestEvent) = {}

  def requestDestroyed(sre: ServletRequestEvent) =
    if (transactionManager.hasLiveTransaction) try {
      tx.commit
      log.debug("Committed current transaction.")
    } catch {
      case e => 
        log.error("An error has occured while trying to commit current transaction.", e)
        tx.rollback
        log.debug("Rolled back current transaction.")
    } finally {
      tx.close
      log.debug("Closed current connection.")
    }
}
