package org.aiotrade.lib.trading.backtest

import java.awt.BorderLayout
import java.awt.Container
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Random
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
import javafx.scene.layout.VBox
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable

/**
 * 
 * @author Caoyuan Deng
 */
class ChartReport(imageFileDirStr: String) extends Reactor {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val df = new SimpleDateFormat("yy.MM.dd")
  private val fileDf = new SimpleDateFormat("yyMMddHHmm")
  private val cssUrl = Thread.currentThread.getContextClassLoader.getResource("chart.css").toExternalForm
  
  private val idToSeries = new mutable.HashMap[String, XYChart.Series[String, Number]]()
  private val width = 1200
  private val height = 900

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
  
  private var frame: JFrame = _
  private var fxPanel: JFXPanel = _
  private var scene: Scene = _
  var dataChart: LineChart[String, Number] = _
  var referChart: LineChart[String, Number] = _
  
  initAndShowGUI

  reactions += {
    case RoundStarted(param) =>
      startNewRound(param)
    case RoundFinished(param) =>
      saveImage(param)
    case data: ReportData => 
      updateData(data)
    case _ =>
  }
  
  private def initAndShowGUI {
    frame = new JFrame()
    
    fxPanel = new JFXPanel()
    frame.add(fxPanel, BorderLayout.CENTER)

    runInFXThread {
      val vbox = new VBox()

      val xAxis = new CategoryAxis()
      xAxis.setLabel("Time")
      val yAxis = new NumberAxis()
      
      dataChart = new LineChart[String, Number](xAxis, yAxis)
      dataChart.setTitle("Profit Monitoring")
      dataChart.setCreateSymbols(false)
      dataChart.setLegendVisible(false)
      dataChart.setPrefHeight(0.9 * height)

      val xAxisRef = new CategoryAxis()
      xAxisRef.setLabel("Time")
      val yAxisRef = new NumberAxis()
      
      referChart = new LineChart[String, Number](xAxisRef, yAxisRef)
      referChart.setCreateSymbols(false)
      referChart.setLegendVisible(false)
      
      vbox.getChildren.add(dataChart)
      vbox.getChildren.add(referChart)
      scene = new Scene(vbox, width, height)
      scene.getStylesheets.add(cssUrl)
          
      fxPanel.setScene(scene)
      frame.pack
      frame.setVisible(true)
    }
  }
  
  private def startNewRound(param: Param) {
    idToSeries.clear
    runInFXThread {
      dataChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
      dataChart.setTitle("Profit Monitoring - " + param)
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
          idToSeries += (id -> x)
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

  private def saveImage(param: Param) {
    if (imageFileDir != null) {
      val file = new File(imageFileDir, fileDf.format(new Date(System.currentTimeMillis)) + "_" + param.shortDescription + ".png")
      val boundbox = new BoundingBox(0, 0, frame.getWidth, frame.getHeight)
      saveImage(frame, boundbox, file)
    }
  }
  
  private def saveImage(container: Container, bounds: Bounds, file: File)  {
    try {
      val name = file.getName
      val ext = name.lastIndexOf(".") match {
        case dot if dot >= 0 => name.substring(dot + 1)
        case _ => "jpg"
      }
      ImageIO.write(toBufferedImage(container, bounds), ext, file)
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
  
  private def runInFXThread(block: => Unit) {
    Platform.runLater(new Runnable {
        def run = block // @Note don't write as: def run {block}
      })
  }
}

object ChartReport {
  
  // -- simple test
  def main(args: Array[String]) {
    val cal = Calendar.getInstance
    val pub = new Publisher {}
    // should hold chartReport instance, otherwise it may be GCed and cannot receive message. 
    val chartReport = new ChartReport(null)
    chartReport.listenTo(pub)
    
    val random = new Random(System.currentTimeMillis)
    cal.add(Calendar.DAY_OF_YEAR, -10)
    for (i <- 1 to 10) {
      cal.add(Calendar.DAY_OF_YEAR, i)
      //chartReport.updateData(ReportData("series", 0, cal.getTimeInMillis, random.nextDouble))
      pub.publish(ReportData("series", 0, cal.getTimeInMillis, random.nextDouble))
    }
  }
}