package org.aiotrade.lib.util.swing.table

import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.{Border, EmptyBorder}
import javax.swing.table.DefaultTableCellRenderer

/**
 * @version 1.0 11/22/98
 */
object AttributiveCellRenderer {
  private val noFocusBorder: Border = new EmptyBorder(1, 1, 1, 1)
}
class AttributiveCellRenderer extends DefaultTableCellRenderer {

  override def getTableCellRendererComponent(table: JTable, value: Object,
                                             isSelected: Boolean, hasFocus: Boolean,
                                             row: Int, column: Int): Component = {
    var foreground: Color = null
    var background: Color = null
    var font: Font = null
    val model = table.getModel
    if (model.isInstanceOf[AttributiveCellTableModel]) {
      val cellAttr = model.asInstanceOf[AttributiveCellTableModel].getCellAttribute
      if (cellAttr.isInstanceOf[ColoredCell]) {
        foreground = cellAttr.asInstanceOf[ColoredCell].getForeground(row, column)
        background = cellAttr.asInstanceOf[ColoredCell].getBackground(row, column)
        setHorizontalAlignment(cellAttr.asInstanceOf[ColoredCell].getHorizontalAlignment(row, column))
      }
      if (cellAttr.isInstanceOf[CellFont]) {
        font = cellAttr.asInstanceOf[CellFont].getFont(row, column)
      }
    }

    if (isSelected) {
      setForeground(if (foreground != null) foreground else table.getSelectionForeground)
      setBackground(table.getSelectionBackground)
    } else {
      setForeground(if (foreground != null) foreground else table.getForeground)
      setBackground(if (background != null) background else table.getBackground)
    }

    setFont(if (font != null) font else table.getFont)

    if (hasFocus) {
      var border: Border = if (isSelected) {
        UIManager.getBorder("Table.focusSelectedCellHighlightBorder")
      } else {
        null
      }
      if (border == null) {
        border = UIManager.getBorder("Table.focusCellHighlightBorder")
      }
      setBorder(border)

      if (!isSelected && table.isCellEditable(row, column)) {
        setForeground(if (foreground != null) foreground else UIManager.getColor("Table.focusCellForeground"))
        setBackground(UIManager.getColor("Table.focusCellBackground"))
      }
    } else {
      setBorder(AttributiveCellRenderer.noFocusBorder)
    }

    setValue(value)

    this
  }

  override protected def setValue(value: Object): Unit = {
    setText(if (value == null) "" else value.toString)
  }
}


