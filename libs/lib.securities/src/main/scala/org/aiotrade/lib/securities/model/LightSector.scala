/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

@serializable
class LightSector extends CRCLongId {

  var category: String = ""
  var code: String = ""
  var name: String = ""
  var childrenString: String = ""
  var parent: LightSector = _
  lazy val key = Sector.toKey(category, code)

  def copyFrom(another: LightSector){
    this.code = another.code
    this.name = another.name
    this.crckey = another.crckey
    this.childrenString = another.childrenString
  }

}
