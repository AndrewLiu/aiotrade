package ru.circumflex.orm

/**
 * Ensures that current transaction is committed and that contextual connection is closed
 * at the end of request processing cycle.
 * This filter should be the first in chain.
 */
import javax.servlet.FilterChain
import org.slf4j.LoggerFactory
import ru.circumflex.core.AbstractCircumflexFilter
import ru.circumflex.core.CircumflexContext
import ORM._

class TransactionManagementFilter extends AbstractCircumflexFilter {
  override protected val log = LoggerFactory.getLogger("ru.circumflex.orm")

  /**
   * Commits current transaction at the end of request processing cycle and closes current connection.
   */
  def doFilter(ctx: CircumflexContext, chain: FilterChain) = {
    chain.doFilter(ctx.request, ctx.response)
    if (transactionManager.hasLiveTransaction ) try {
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
}