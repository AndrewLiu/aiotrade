/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.data

import java.util.TimerTask
import java.util.logging.Level
import java.util.logging.Logger

object SyncExportToAvro extends SyncBase{
  private val log = Logger.getLogger(this.getClass.getName)
  def main(args: Array[String]){
    val date = getNearestTime(9, 0)
    println("Will export data from db at " + date)
    timer.scheduleAtFixedRate(new TimerTask(){
        def run(){
          try{
            exportAvroDataFileFromProductionMysql
          }
          catch{
            case ex =>log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      }, date, 12 * 3600 * 1000)

  }
}