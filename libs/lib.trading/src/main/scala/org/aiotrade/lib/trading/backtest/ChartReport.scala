package org.aiotrade.lib.trading.backtest

import java.awt.BorderLayout
import java.awt.Container
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.Logger
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.embed.swing.JFXPanel
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.VBox
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.Timer
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class ChartReport(imageFileDirStr: String, isAutoRanging: Boolean = true, 
                  upperBound: Int = 0, lowerBound: Int = 1000, 
                  width: Int = 1200, height: Int = 900
) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val cssUrl = Thread.currentThread.getContextClassLoader.getResource("chart.css").toExternalForm
  private val imageFileDir = {
    if (imageFileDirStr != null) {
      try {
        val dir = new File(imageFileDirStr)
        if (!dir.exists) {
          dir.mkdirs
        }
        dir
      } catch {
        case ex => 
          log.log(Level.WARNING, ex.getMessage, ex)
          null
      }
    } else null
  }
  
  private var imageSavingLatch: CountDownLatch = _
  private var chartTabs = List[ChartTab]()
  
  private val frame = new JFrame()
  private val jfxPanel = new JFXPanel()
  private val tabPane = new TabPane()
  
  initAndShowGUI
  
  private def runInFXThread(block: => Unit) {
    Platform.runLater(new Runnable {
        def run = block // @Note don't write as: def run {block}
      })
  }
  
  private def initAndShowGUI {
    // should put this code outside of runInFXThread, otherwise will cause: Toolkit not initialized
    frame.add(jfxPanel, BorderLayout.CENTER)
    
    runInFXThread {
      val scene = new Scene(tabPane, width, height)
      scene.getStylesheets.add(cssUrl)
      jfxPanel.setScene(scene)

      frame.pack
      frame.setVisible(true)
    }
  }
  
  /**
   * @param for each param in params, will create a new tabbed pane in main frame.
   */
  def roundStarted(params: List[Param]) {
    if (imageSavingLatch != null) {
      try {
        imageSavingLatch.await
      } catch {
        case e: InterruptedException => e.printStackTrace
      }
    }
    chartTabs = params map (new ChartTab(_))
    Thread.sleep(1000) // wait for chartTab inited in FX thread
  }
  
  def roundFinished {
    imageSavingLatch = new CountDownLatch(chartTabs.length)
    Thread.sleep(2000) // wait for chart painted in FX thread
    trySaveNextImage
  }
  
  private def trySaveNextImage {
    chartTabs match {
      case Nil =>
      case x :: xs =>
        chartTabs = xs
        x.roundFinished
    }
  }
  
  private class ChartTab(param: Param) extends Reactor {
    private val idToSeries = new mutable.HashMap[String, XYChart.Series[String, Number]]()
    private var dataChart: LineChart[String, Number] = _
    private var referChart: LineChart[String, Number] = _

    private val df = new SimpleDateFormat("yy.MM.dd")
    private val fileDf = new SimpleDateFormat("yyMMddHHmm")

    val tab = new Tab()
    tab.setText(param.titleDescription)
    private var root: VBox = _
    
    initAndShowGUI
  
    reactions += {
      case data: ReportData => 
        updateData(data)
    }
    listenTo(param)
  
    private def initAndShowGUI {
      runInFXThread {
        root = new VBox()

        val xAxis = new CategoryAxis()
        xAxis.setLabel("Time")
        val yAxis = new NumberAxis()
        yAxis.setAutoRanging(isAutoRanging)
        yAxis.setUpperBound(upperBound)
        yAxis.setLowerBound(lowerBound)
      
        dataChart = new LineChart[String, Number](xAxis, yAxis)
        dataChart.setTitle("Profit Monitoring - " + param.titleDescription)
        dataChart.setCreateSymbols(false)
        dataChart.setLegendVisible(false)
        dataChart.setPrefHeight(0.9 * height)

        val xAxisRef = new CategoryAxis()
        xAxisRef.setLabel("Time")
        val yAxisRef = new NumberAxis()
      
        referChart = new LineChart[String, Number](xAxisRef, yAxisRef)
        referChart.setCreateSymbols(false)
        referChart.setLegendVisible(false)
      
        root.getChildren.add(dataChart)
        root.getChildren.add(referChart)
        tab.setContent(root)

        tabPane.getTabs.add(tab)
      }
    }
  
    private def resetData {
      runInFXThread {
        idToSeries.clear
        dataChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
        referChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
      }
    }
  
    private def updateData(data: ReportData) {
      // should run in FX application thread
      runInFXThread {
        val id = data.name + data.id
        val series = idToSeries.getOrElse(id, {
            val x = if (data.name.contains("Refer")) {
              createSeries(data.name + "-" + data.id, true)
            } else {
              createSeries(data.name + "-" + data.id)
            }
            idToSeries(id) = x
            x
          }
        )
        series.getData.add(new XYChart.Data(df.format(new Date(data.time)), data.value))
      }
    }
  
    private def createSeries(name: String, isRefer: Boolean = false): XYChart.Series[String, Number] = {
      val series = new XYChart.Series[String, Number]()
      series.setName(name)
      (if (isRefer) referChart else dataChart).getData.add(series)
      series
    }
  
    def roundFinished {
      if (imageFileDir != null) {
        tabPane.getSelectionModel.select(tab)

        val file = new File(imageFileDir, fileDf.format(new Date(System.currentTimeMillis)) + "_" + param.shortDescription + ".png")
        var timer: Timer = null
        timer = new Timer(1500, new ActionListener {
            def actionPerformed(e: java.awt.event.ActionEvent) {
              ChartReport.saveImage(jfxPanel, file)
              tabPane.getTabs.remove(tab)
              imageSavingLatch.countDown
              trySaveNextImage
              timer.stop
            }
          }
        )
        timer.start
      }
    }
  }
  
}


object ChartReport {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private[backtest] def saveImage(container: Container, file: File)  {
    try {
      val name = file.getName
      val ext = name.lastIndexOf(".") match {
        case dot if dot >= 0 => name.substring(dot + 1)
        case _ => "jpg"
      }
      val boundbox = new BoundingBox(0, 0, container.getWidth, container.getHeight)
      ImageIO.write(toBufferedImage(container, boundbox), ext, file)
      println("=== Image saved ===")
    } catch {
      case ex =>
        log.log(Level.WARNING, ex.getMessage, ex)
        JOptionPane.showMessageDialog(null, "The image couldn't be saved", "Error", JOptionPane.ERROR_MESSAGE)
    }
  }
  
  /**
   * This function is used to get the BufferedImage of the container as JFXPanel etc
   * @param container
   * @param bounds
   * @return
   */
  private def toBufferedImage(container: Container, bounds: Bounds): BufferedImage = {
    val bufferedImage = new BufferedImage(bounds.getWidth.toInt,
                                          bounds.getHeight.toInt,
                                          BufferedImage.TYPE_INT_ARGB)

    val g = bufferedImage.getGraphics
    g.translate(-bounds.getMinX.toInt, -bounds.getMinY.toInt) // translating to upper-left corner
    container.paint(g)
    g.dispose
    bufferedImage
  }
  

  // -- simple test
  def main(args: Array[String]) {
    val cal = Calendar.getInstance
    // should hold chartReport instance, otherwise it may be GCed and cannot receive message. 
    val chartReport = new ChartReport(".")
    val params = List(TestParam(1), TestParam(2))
    chartReport.roundStarted(params)
    
    val random = new Random(System.currentTimeMillis)
    cal.add(Calendar.DAY_OF_YEAR, -10)
    for (i <- 1 to 10) {
      cal.add(Calendar.DAY_OF_YEAR, i)
      params foreach {_.publish(ReportData("series", 0, cal.getTimeInMillis, random.nextDouble))}
    }
    
    chartReport.roundFinished
  }
  
  private case class TestParam(v: Int) extends Param
}