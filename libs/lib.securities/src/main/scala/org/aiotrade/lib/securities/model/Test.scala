/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

object Test {
  def main(args: Array[String]) {
    val N   = Exchange("N",  "America/New_York", Array(9, 30, 14, 30))  // New York
    val L   = Exchange("L",  "UTC", Array(8, 00, 15, 30)) // London


    val SS  = Exchange("SS", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shanghai
    SS.name = "上交所"
    SS.fullName = "上海证券交易所"
    val SZ  = Exchange("SZ", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shenzhen
    SZ.name = "深交所"
    SZ.name = "深圳证券交易所"
    Exchanges.save(SS)
    Exchanges.save(SZ)
  }
}
