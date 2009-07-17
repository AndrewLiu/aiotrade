/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.Dimension;

/**
 * @version 1.0 11/22/98
 */
trait CellAttribute {

  def addColumn :Unit

  def addRow :Unit

  def insertRow(row:Int) :Unit

  def getSize :Dimension

  def setSize(size:Dimension) :Unit
}
