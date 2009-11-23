/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;

/**
 * @version 1.0 11/26/98
 */
class MultiSpanCellTableUI extends BasicTableUI {

  override def paint(g: Graphics, c: JComponent): Unit = {
    val oldClipBounds = g.getClipBounds
    val clipBounds = new Rectangle(oldClipBounds)
    val tableWidth = table.getColumnModel.getTotalColumnWidth
    clipBounds.width = math.min(clipBounds.width, tableWidth)
    g.setClip(clipBounds)

    val firstIndex = table.rowAtPoint(new Point(0, clipBounds.y))
    val lastIndex = table.getRowCount - 1

    val rowRect = new Rectangle(0, 0,
                                tableWidth, table.getRowHeight + table.getRowMargin)
    rowRect.y = firstIndex * rowRect.height

    for (index <- firstIndex until lastIndex) {
      if (rowRect.intersects(clipBounds)) {
        //System.out.println();                  // debug
        //System.out.print("" + index +": ");    // row
        paintRow(g, index)
      }
      rowRect.y += rowRect.height
    }
    g.setClip(oldClipBounds)
  }

  private def paintRow(g: Graphics, row: Int): Unit = {
    val rect = g.getClipBounds
    var drawn = false

    val tableModel = table.getModel.asInstanceOf[AttributiveCellTableModel]
    val cellAtt = tableModel.getCellAttribute.asInstanceOf[CellSpan]
    val numColumns = table.getColumnCount

    def loop(column:Int) {
      if (column < numColumns) {
        val cellRect = table.getCellRect(row, column, true)
        val(cellRow, cellColumn) = if (cellAtt.isVisible(row, column)) {
          (row, column)
          //  System.out.print("   "+column+" ");  // debug
        } else {
          val cellRow1 = row + cellAtt.getSpan(row, column)(CellSpan.ROW)
          val cellColumn1 = column + cellAtt.getSpan(row, column)(CellSpan.COLUMN)
          (cellRow1, cellColumn1)
          //  System.out.print("  ("+column+")");  // debug
        }

        if (cellRect.intersects(rect)) {
          drawn = true
          paintCell(g, cellRect, cellRow, cellColumn)
          loop(column + 1)
        } else {
          if (drawn) { // break
          } else {
            loop(column + 1)
          }
        }
      }
    }
    loop(0)
  }

  private def paintCell(g: Graphics, cellRect: Rectangle, row: Int, column: Int): Unit = {
    val spacingHeight = table.getRowMargin
    val spacingWidth = table.getColumnModel.getColumnMargin

    val c = g.getColor
    g.setColor(table.getGridColor)
    val x1 = cellRect.x
    val y1 = cellRect.y
    val x2 = cellRect.x + cellRect.width - 1
    val y2 = cellRect.y + cellRect.height - 1
    if (table.getShowHorizontalLines) {
      g.drawLine(x1, y1, x2, y1)
      g.drawLine(x1, y2, x2, y2)
    }
    if (table.getShowVerticalLines) {
      g.drawLine(x1, y1, x1, y2)
      g.drawLine(x2, y1, x2, y2)
    }
    g.setColor(c)

    cellRect.setBounds(cellRect.x + spacingWidth / 2, cellRect.y + spacingHeight / 2,
                       cellRect.width - spacingWidth, cellRect.height - spacingHeight)

    if (table.isEditing() && table.getEditingRow() == row &&
        table.getEditingColumn() == column) {
      val component = table.getEditorComponent
      component.setBounds(cellRect)
      component.validate
    } else {
      val renderer = table.getCellRenderer(row, column)
      val component = table.prepareRenderer(renderer, row, column)

      if (component.getParent == null) {
        rendererPane.add(component)
      }
      rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                                  cellRect.width, cellRect.height, true)
    }
  }
}

