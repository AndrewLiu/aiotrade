package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import java.util.logging.Logger
import java.util.logging.Level
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.PersistenceManager


class QuoteInfoHisContract extends DataContract[QuoteInfo, QuoteInfoHisDataServer] {
  val log = Logger.getLogger(this.getClass.getName)

  serviceClassName = null
  freq = TFreq.DAILY
  refreshable = false

  override def displayName = {
    "QuoteInfo His Data Contract[" + srcSymbol + "]"
  }

  /**
   * @param none args are needed
   */
  override def createServiceInstance(args: Any*): Option[QuoteInfoHisDataServer] = {
    lookupServiceTemplate match {
      case Some(x) => x.createNewInstance.asInstanceOf[Option[QuoteInfoHisDataServer]]
      case None => None
    }
  }

  def lookupServiceTemplate: Option[QuoteInfoHisDataServer] = {
    val services = PersistenceManager().lookupAllRegisteredServices(classOf[QuoteInfoHisDataServer], "InfoServers")
    services find {x =>
      val className = x.getClass.getName
      className == serviceClassName || (className + "$") == serviceClassName
    } match {
      case None =>
        try {
          log.warning("Cannot find registeredService of QuoteInfoHisDataServer in " + (services map (_.getClass.getName)) + ", try Class.forName call: serviceClassName=" + serviceClassName)
          Some(Class.forName(serviceClassName).newInstance.asInstanceOf[QuoteInfoHisDataServer])
        } catch {
          case ex: Exception => log.log(Level.SEVERE, "Cannot class.forName of class: " + serviceClassName, ex); None
        }
      case some => some
    }
  }
}
