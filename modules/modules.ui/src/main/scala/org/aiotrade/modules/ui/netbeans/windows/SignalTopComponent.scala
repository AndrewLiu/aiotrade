package org.aiotrade.modules.ui.netbeans.windows

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.signal.Sign
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SubSignalEvent
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.modules.ui.netbeans.nodes.SymbolNodes
import org.aiotrade.lib.util.swing.action.ViewAction
import org.openide.windows.TopComponent

class SignalTopComponent extends TopComponent with Reactor {
  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.netbeans.windows.Bundle")

  private val tc_id = "SignalTopComponent"

  private var signalTable: JTable = _
  private var signalModel: AbstractTableModel = _

  private val signalEvents = new ArrayList[SubSignalEvent]

  initComponent

  reactions += {
    case x@SubSignalEvent(uniSymbol, name, freq, signal) =>
      requestActive
      updateSignalTable(x)
      signalTable.repaint()
  }
  listenTo(Signal)
  
  private def initComponent {
    
    signalModel = new AbstractTableModel {
      private val columnNames = Array[String](
        Bundle.getString("time"), Bundle.getString("symbol"), Bundle.getString("sign"), Bundle.getString("name")
      )

      def getRowCount: Int = signalEvents.size
      def getColumnCount: Int = columnNames.length

      def getValueAt(row: Int, col: Int): Object = {
        val event = signalEvents(row)
        col match {
          case 0 =>
            val sec = Exchange.secOf(event.uniSymbol).getOrElse(return null)
            val timeZone = sec.exchange.timeZone
            val cal = Calendar.getInstance(timeZone)
            val df = new SimpleDateFormat("HH:mm:ss")
            df.setTimeZone(timeZone)

            cal.setTimeInMillis(event.signal.time)
            df format cal.getTime
          case 1 => event.uniSymbol
          case 2 => event.signal.sign match {
              case Sign.EnterLong  => Bundle.getString("enterLong")
              case Sign.ExitLong   => Bundle.getString("exitLong")
              case Sign.EnterShort => Bundle.getString("enterShort")
              case Sign.ExitShort  => Bundle.getString("exitShort")
            }
          case 3 => event.name
          case _ => null
        }
      }

      override def getColumnName(col: Int) = columnNames(col)
    }

    signalTable = new JTable(signalModel)
    //executionTable.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    signalTable.setFocusable(false)
    signalTable.setCellSelectionEnabled(false)
    signalTable.setShowHorizontalLines(false)
    signalTable.setShowVerticalLines(false)
    //signalTable.setForeground(Color.BLACK)
    //signalTable.setBackground(Color.WHITE)
    signalTable.setFillsViewportHeight(true)
    signalTable.getTableHeader.setDefaultRenderer(new TableHeaderRenderer)

    val scrollPane = new JScrollPane
    scrollPane.setViewportView(signalTable)
    //scrollPane.setBackground(Color.WHITE)
    scrollPane.setFocusable(true)

    setLayout(new BorderLayout)
    add(BorderLayout.CENTER, scrollPane)

    signalTable.getSelectionModel.addListSelectionListener(new ListSelectionListener {
        private var prevSelected: String = _
        def valueChanged(e: ListSelectionEvent) {
          val lsm = e.getSource.asInstanceOf[ListSelectionModel]
          if (lsm.isSelectionEmpty) {
            // no rows are selected
          } else {
            val row = signalTable.getSelectedRow
            if (row >= 0 && row < signalTable.getRowCount) {
              val symbol = symbolAtRow(row)
              if (symbol != null && prevSelected != symbol) {
                prevSelected = symbol
                SymbolNodes.findSymbolNode(symbol) foreach {x =>
                  val viewAction = x.getLookup.lookup(classOf[ViewAction])
                  viewAction.putValue(AnalysisChartTopComponent.STANDALONE, false)
                  viewAction.execute
                }
              }
            }
          }
        }
      })

  }

  def symbolAtRow(row: Int): String = {
    if (row >= 0 && row < signalModel.getRowCount) {
      signalTable.getValueAt(row, 1).asInstanceOf[String]
    } else null
  }

  /**
   * Update last execution row in depth table
   */
  private def updateSignalTable(event: SubSignalEvent) {
    signalEvents += event
    signalModel.fireTableDataChanged
    scrollToLastRow(signalTable)
  }

  private def scrollToLastRow(table: JTable) {
    if (table.getRowCount < 1) return

    // wrap in EDT to wait enough time to get rowCount updated
    SwingUtilities.invokeLater(new Runnable {
        def run {
          showCell(table, table.getRowCount - 1, 0)
        }
      })
  }

  private def showCell(table: JTable, row: Int, column: Int) {
    val rect = table.getCellRect(row, column, true)
    table.scrollRectToVisible(rect)
    table.clearSelection
    table.setRowSelectionInterval(row, row)
  }

  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_ALWAYS
  }

  override protected def preferredID: String = tc_id

  override def getDisplayName: String = Bundle.getString("CTL_SignalTopComponent")


  class TableHeaderRenderer extends JLabel with TableCellRenderer {
    private val defaultFont = new Font("Dialog", Font.BOLD, 12)
    setOpaque(true)
    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      //setForeground(Color.BLACK)
      //setBackground(Color.WHITE)
      setFont(defaultFont)
      setHorizontalAlignment(SwingConstants.CENTER)
      setText(value.toString)

      setToolTipText(value.toString)

      this
    }

    // The following methods override the defaults for performance reasons
    override def validate {}
    override def revalidate() {}
    override protected def firePropertyChange(propertyName: String, oldValue: Object, newValue: Object) {}
    override def firePropertyChange(propertyName: String, oldValue: Boolean, newValue: Boolean) {}
  }

}
