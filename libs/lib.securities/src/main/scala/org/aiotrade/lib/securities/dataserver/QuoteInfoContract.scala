/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import java.util.logging.Logger
import java.util.logging.Level
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.PersistenceManager

class QuoteInfoContract extends DataContract[QuoteInfo, QuoteInfoDataServer] {
  val log = Logger.getLogger(this.getClass.getName)

  serviceClassName = null
  freq = TFreq.ONE_MIN
  refreshable = true

  override def displayName = {
    "QuoteInfo Data Contract[" + srcSymbol + "]"
  }

  /**
   * @param none args are needed
   */
  override def createServiceInstance(args: Any*): Option[QuoteInfoDataServer] = {
    lookupServiceTemplate match {
      case Some(x) => x.createNewInstance.asInstanceOf[Option[QuoteInfoDataServer]]
      case None => None
    }
  }

  def lookupServiceTemplate: Option[QuoteInfoDataServer] = {
    val services = PersistenceManager().lookupAllRegisteredServices(classOf[QuoteInfoDataServer], "InfoServers")
    services find {x => x.getClass.getName == serviceClassName} match {
      case None =>
        try {
          log.warning("Cannot find registeredService of QuoteInfoDataServer in " + services + ", try Class.forName call: serviceClassName=" + serviceClassName)
          Some(Class.forName(serviceClassName).newInstance.asInstanceOf[QuoteInfoDataServer])
        } catch {
          case ex: Exception => log.log(Level.SEVERE, "Cannot class.forName of class: " + serviceClassName, ex); None
        }
      case some => some
    }
  }
}
