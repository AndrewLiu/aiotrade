package org.aiotrade.lib.util.swing.table

import java.awt.Dimension
import java.util.Vector
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel

/**
 * @version 1.0 11/22/98
 */
object AttributiveCellTableModel {
  private def nonNullVector[T](v: Vector[T]): Vector[T] = {
    if (v != null) v else new Vector[T]
  }

  /**
   * Returns a vector that contains the same objects as the array.
   * @param anArray  the array to be converted
   * @return  the new vector; if <code>anArray</code> is <code>null</code>,
   *                          returns <code>null</code>
   */
  def convertToVector(anArray: Array[Any]): Vector[_] = {
    if (anArray == null) {
      return null
    }
    val v = new Vector[Any](anArray.length)
    anArray foreach {x => v.addElement(x)}
    v
  }

  /**
   * Returns a vector of vectors that contains the same objects as the array.
   * @param anArray  the double array to be converted
   * @return the new vector of vectors; if <code>anArray</code> is
   *                          <code>null</code>, returns <code>null</code>
   */
  def convertToVector(anArray: Array[Array[Any]]): Vector[Vector[_]] = {
    if (anArray == null) {
      return null
    }
    val v = new Vector[Vector[_]](anArray.length)
    anArray foreach {x => v.addElement(convertToVector(x))}
    v
  }

}
class AttributiveCellTableModel(columnNames: Vector[_], numRows: Int, numColumns: Int) extends DefaultTableModel {
  import AttributiveCellTableModel._

  if (columnNames != null) {
    setColumnIdentifiers(columnNames)
  } else {
    val names = new Vector[AnyRef](numColumns)
    names.setSize(numColumns)
    setColumnIdentifiers(names)
  }
  dataVector = new Vector[AnyRef]
  setNumRows(numRows)
  
  protected var cellAtt: CellAttribute = new DefaultCellAttribute(numRows, numColumns)

  def this() = {
    this(null, 0, 0)
  }

  def this(numRows: Int, numColumns: Int) = {
    this(null, numRows, numColumns)
  }

  def this(columnNames: Vector[_], numRows: Int) {
    this(columnNames, 0, numRows)
  }

  def this(data: Vector[_], columnNames: Vector[_]) = {
    this(columnNames, columnNames.size, data.size)
    setDataVector(data, columnNames)
  }

  def this(columnNames: Array[Any], numRows: Int) = {
    this(AttributiveCellTableModel.convertToVector(columnNames), numRows)
  }

  def this(data: Array[Array[Any]], columnNames: Array[Any]) = {
    this(AttributiveCellTableModel.convertToVector(columnNames), columnNames.size, 0)
    setDataVector(AttributiveCellTableModel.convertToVector(data), AttributiveCellTableModel.convertToVector(columnNames))
  }

  override def setDataVector(newData: Vector[_], columnNames: Vector[_]): Unit = {
    if (newData == null) {
      throw new IllegalArgumentException("setDataVector) - Null parameter")
    }
    dataVector = new Vector[AnyRef](0)
    columnIdentifiers = nonNullVector(columnNames)
    dataVector = newData

    cellAtt = new DefaultCellAttribute(dataVector.size, columnIdentifiers.size)

    newRowsAdded(new TableModelEvent(this, 0, getRowCount - 1,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  override def addColumn(columnName: AnyRef, columnData: Vector[_]): Unit = {
    if (columnName == null) {
      throw new IllegalArgumentException("addColumn() - null parameter")
    }
    columnIdentifiers.asInstanceOf[Vector[AnyRef]].addElement(columnName)
    var index = 0
    val enumeration = dataVector.elements
    while (enumeration.hasMoreElements) {
      var value: AnyRef = null
      if ((columnData != null) && (index < columnData.size)) {
        value = columnData.asInstanceOf[Vector[AnyRef]].elementAt(index)
      } else {
        value = null
      }
      enumeration.nextElement.asInstanceOf[Vector[AnyRef]].addElement(value)
      index += 1
    }

    //
    cellAtt.addColumn

    fireTableStructureChanged
  }

  override def addRow(rowData: Vector[_]): Unit = {
    var newData: Vector[_] = null
    if (rowData == null) {
      newData = new Vector[AnyRef](getColumnCount)
    } else {
      rowData.setSize(getColumnCount)
    }
    dataVector.asInstanceOf[Vector[AnyRef]].addElement(newData)

    //
    cellAtt.addRow

    newRowsAdded(new TableModelEvent(this, getRowCount() - 1, getRowCount() - 1,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  override def insertRow(row: Int, arowData: Vector[_]): Unit = {
    var rowData = arowData
    if (arowData == null) {
      rowData = new Vector[AnyRef](getColumnCount)
    } else {
      rowData.setSize(getColumnCount)
    }

    dataVector.asInstanceOf[Vector[AnyRef]].insertElementAt(rowData, row)

    cellAtt.insertRow(row)

    newRowsAdded(new TableModelEvent(this, row, row,
                                     TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT))
  }

  def getCellAttribute: CellAttribute = {
    cellAtt
  }

  def setCellAttribute(newCellAtt: CellAttribute): Unit = {
    val numColumns = getColumnCount
    val numRows = getRowCount
    if ((newCellAtt.getSize.width != numColumns) ||
        (newCellAtt.getSize.height != numRows)) {
      newCellAtt.setSize(new Dimension(numRows, numColumns))
    }
    cellAtt = newCellAtt
    fireTableDataChanged
  }

  /*
   public void changeCellAttribute(int row, int column, Object command) {
   cellAtt.changeAttribute(row, column, command);
   }

   public void changeCellAttribute(int[] rows, int[] columns, Object command) {
   cellAtt.changeAttribute(rows, columns, command);
   }
   */
}

