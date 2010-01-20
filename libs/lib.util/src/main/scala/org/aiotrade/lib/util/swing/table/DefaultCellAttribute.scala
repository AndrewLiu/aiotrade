/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.SwingConstants

/**
 * @version 1.0 11/22/98
 */
abstract class AttrType
object AttrType {
  case object Foreground extends AttrType
  case object Background extends AttrType
  case object Font extends AttrType
  case object HorizontalAlignment extends AttrType
  case object VerticalAlignment extends AttrType
}

class DefaultCellAttribute(numRows: Int, numColumns: Int) extends CellAttribute with CellSpan with ColoredCell with CellFont {

  private class Attr {
    var foreground: Color = _
    var background: Color = _
    var font: Font = _
    var horizontalAlignment: Int = _
    var verticalAlignment: Int = _
  }


  //
  // !!!! CAUTION !!!!!
  // these values must be synchronized to Table data
  //
  protected var rowSize: Int = _
  protected var columnSize: Int = _
  protected var spans: Array[Array[Array[Int]]] = _                 // CellSpan
  private var attrs: Array[Array[Attr]] = _

  setSize(new Dimension(numColumns, numRows))

  def this() = {
    this(1, 1)
  }


  protected def initValue: Unit = {
    for (i <- 0 until spans.length) {
      for (j <- 0 until spans(i).length) {
        spans(i)(j)(CellSpan.COLUMN) = 1
        spans(i)(j)(CellSpan.ROW) = 1
      }
    }
  }

  //
  // CellSpan
  //
  def getSpan(row: Int, column: Int): Array[Int] = {
    if (isOutOfBounds(row, column)) {
      Array(1, 1)
    } else {
      spans(row)(column)
    }
  }

  def setSpan(span: Array[Int], row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return
    }
    spans(row)(column) = span
  }

  def isVisible(row: Int, column: Int): Boolean = {
    if (isOutOfBounds(row, column)) {
      return false
    }
    if (spans(row)(column)(CellSpan.COLUMN) < 1 || spans(row)(column)(CellSpan.ROW) < 1) {
      false
    } else {
      true
    }
  }

  def combine(rows: Array[Int], columns: Array[Int]) {
    if (isOutOfBounds(rows, columns)) {
      return
    }
    val rowSpan = rows.length;
    val columnSpan = columns.length
    val startRow = rows(0)
    val startColumn = columns(0)
    
    for (i <- 0 until rowSpan) {
      for (j <- 0 until columnSpan) {
        if ((spans(startRow + i)(startColumn + j)(CellSpan.COLUMN) != 1) ||
            (spans(startRow + i)(startColumn + j)(CellSpan.ROW) != 1)) {
          //System.out.println("can't combine");
          return
        }
      }
    }
    
    var ii = 0
    for (i <- 0 until rowSpan) {
      var jj = 0
      for (j <- 0 until columnSpan) {
        spans(startRow + i)(startColumn + j)(CellSpan.COLUMN) = jj
        spans(startRow + i)(startColumn + j)(CellSpan.ROW) = ii
        //System.out.println("r " +ii +"  c " +jj);
        jj -= 1
      }
      ii -= 1
    }
    
    spans(startRow)(startColumn)(CellSpan.COLUMN) = columnSpan
    spans(startRow)(startColumn)(CellSpan.ROW) = rowSpan

  }

  def split(row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return;
    }
    val columnSpan = spans(row)(column)(CellSpan.COLUMN)
    val rowSpan = spans(row)(column)(CellSpan.ROW)
    for (i <- 0 until rowSpan) {
      for (j <- 0 until columnSpan) {
        spans(row + i)(column + j)(CellSpan.COLUMN) = 1
        spans(row + i)(column + j)(CellSpan.ROW) = 1
      }
    }
  }

  //
  // ColoredCell
  //
  def getForeground(row: Int, column: Int): Color = {
    if (isOutOfBounds(row, column)) {
      return null;
    }
    val attr = attrs(row)(column)
    if (attr == null) {
      null
    } else {
      attr.foreground
    }
  }

  def setForeground(color: Color, row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return;
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      attr = new Attr
      attrs(row)(column) = attr
    }
    attr.foreground = color;
  }

  def setForeground(color: Color, rows: Array[Int], columns: Array[Int]) {
    if (isOutOfBounds(rows, columns)) {
      return
    }
    setAttributes(AttrType.Foreground, color, rows, columns)
  }

  def getBackground(row: Int, column: Int): Color = {
    if (isOutOfBounds(row, column)) {
      return null;
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      return null;
    } else {
      return attr.background;
    }
  }

  def setBackground(color: Color, row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return;
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      attr = new Attr()
      attrs(row)(column) = attr
    }
    attr.background = color
  }

  def setBackground(color: Color, rows: Array[Int], columns: Array[Int]) {
    if (isOutOfBounds(rows, columns)) {
      return;
    }
    setAttributes(AttrType.Background, color, rows, columns)
  }
  //

  //
  // CellFont
  //
  def getFont(row: Int, column: Int): Font = {
    if (isOutOfBounds(row, column)) {
      return null;
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      null
    } else {
      attr.font
    }
  }

  def setFont(font: Font, row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      attr = new Attr
      attrs(row)(column) = attr
    }
    attr.font = font
  }

  def setFont(font: Font, rows: Array[Int], columns: Array[Int]) {
    if (isOutOfBounds(rows, columns)) {
      return;
    }
    setAttributes(AttrType.Font, font, rows, columns)
  }

  def getHorizontalAlignment(row: Int, column: Int): Int = {
    if (isOutOfBounds(row, column)) {
      return SwingConstants.LEADING
    }
    val attr = attrs(row)(column)
    if (attr == null) {
      SwingConstants.LEADING
    } else {
      attr.horizontalAlignment
    }
  }

  def setHorizontalAlignment(horizontalAlignment: Int, row: Int, column: Int) {
    if (isOutOfBounds(row, column)) {
      return
    }
    var attr = attrs(row)(column)
    if (attr == null) {
      attr = new Attr
      attrs(row)(column) = attr
    }
    attr.horizontalAlignment = horizontalAlignment
  }
  //

  //
  // CellAttribute
  //
  def addColumn {
    val oldSpan = spans
    val numRows = oldSpan.length
    val numColumns = oldSpan(0).length
    spans = Array.ofDim(numRows, numColumns + 1, 2)
    System.arraycopy(oldSpan, 0, spans, 0, numRows)
    for (i <- 0 until numRows) {
      spans(i)(numColumns)(CellSpan.COLUMN) = 1
      spans(i)(numColumns)(CellSpan.ROW) = 1
    }

    val oldAttr = attrs;
    attrs = Array.ofDim(numRows, numColumns + 1)
    System.arraycopy(oldAttr, 0, attrs, 0, numRows);
  }

  def addRow {
    val oldSpan = spans
    val numRows = oldSpan.length;
    val numColumns = oldSpan(0).length;

    spans = Array.ofDim(numRows + 1, numColumns, 2)
    System.arraycopy(oldSpan, 0, spans, 0, numRows)
    for (i <- 0 until numColumns) {
      spans(numRows)(i)(CellSpan.COLUMN) = 1
      spans(numRows)(i)(CellSpan.ROW) = 1
    }

    val oldAttr = attrs
    attrs = Array.ofDim(numRows, numColumns + 1)
    System.arraycopy(oldAttr, 0, attrs, 0, numRows)
  }

  def insertRow(row: Int) {
    val oldSpan = spans
    val numRows = oldSpan.length
    val numColumns = oldSpan(0).length
    spans = Array.ofDim(numRows + 1, numColumns, 2)
    if (0 < row) {
      System.arraycopy(oldSpan, 0, spans, 0, row - 1)
    }
    System.arraycopy(oldSpan, 0, spans, row, numRows - row)
    for (i <- 0 until numColumns) {
      spans(row)(i)(CellSpan.COLUMN) = 1
      spans(row)(i)(CellSpan.ROW) = 1
    }
  }

  def getSize: Dimension = {
    return new Dimension(rowSize, columnSize)
  }

  def setSize(size: Dimension) {
    columnSize = size.width
    rowSize = size.height
    spans = Array.ofDim(rowSize, columnSize, 2)   // 2: COLUMN,ROW
    attrs = Array.ofDim(rowSize, columnSize)
    initValue
  }

  /*
   public void changeAttribute(int row, int column, Object command) {
   }

   public void changeAttribute(int[] rows, int[] columns, Object command) {
   }
   */
  protected def isOutOfBounds(row: Int, column: Int): Boolean = {
    if ((row < 0) || (rowSize <= row) || (column < 0) || (columnSize <= column)) {
      true
    } else {
      false
    }
  }

  protected def isOutOfBounds(rows: Array[Int], columns: Array[Int]): Boolean = {
    for (i <- 0 until rows.length) {
      if ((rows(i) < 0) || (rowSize <= rows(i))) {
        return true
      }
    }
    for (i <- 0 until columns.length) {
      if ((columns(i) < 0) || (columnSize <= columns(i))) {
        return true
      }
    }

    false
  }

  private def setAttributes(tpe:AttrType, value: Object, rows: Array[Int], columns: Array[Int]) {
    for (i <- 0 until rows.length) {
      val row = rows(i)
      for (j <- 0 until columns.length) {
        val column = columns(j)
        var attr = attrs(row)(column)
        if (attr == null) {
          attr = new Attr
          attrs(row)(column) = attr
        }
        
        tpe match {
          case AttrType.Foreground =>
            attr.foreground = value.asInstanceOf[Color]
          case AttrType.Background =>
            attr.background = value.asInstanceOf[Color]
          case AttrType.Font =>
            attr.font = value.asInstanceOf[Font]
          case AttrType.HorizontalAlignment =>
            attr.horizontalAlignment = value.asInstanceOf[Int]
          case AttrType.VerticalAlignment =>
            attr.verticalAlignment = value.asInstanceOf[Int]
        }
      }
    }
  }
}
