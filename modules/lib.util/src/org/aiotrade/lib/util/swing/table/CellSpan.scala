/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

/**
 * @version 1.0 11/22/98
 */
object CellSpan {
  val ROW = 0
  val COLUMN = 1
}

trait CellSpan {

  def getSpan(row: Int, column: Int): Array[Int]

  def setSpan(span: Array[int], row: Int, column: Int): Unit

  def isVisible(row: Int, column: Int) :Boolean

  def combine(rows: Array[Int], columns: Array[Int]): Unit

  def split(row: Int, column: Int): Unit
}
