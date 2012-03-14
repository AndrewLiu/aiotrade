package org.aiotrade.lib.backtest

import java.awt.BorderLayout
import java.awt.Container
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
class ChartReport(dataPublisher: Publisher, imageFileDirStr: String) extends Reactor {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val df = new SimpleDateFormat("yy.MM.dd")
  private val fileDf = new SimpleDateFormat("yyMMddHHmm")
  private val cssUrl = Thread.currentThread.getContextClassLoader.getResource("chart.css").toExternalForm
  
  private val idToSeries = new mutable.HashMap[String, XYChart.Series[String, Number]]()

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
  private var lineChart: LineChart[String, Number] = _
  
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
  listenTo(dataPublisher)
  
  private def initAndShowGUI {
    frame = new JFrame()
    
    fxPanel = new JFXPanel()
    frame.add(fxPanel, BorderLayout.CENTER)

    runInFXThread {
      val xAxis = new CategoryAxis()
      //xAxis.setAutoRanging(false)
      //xAxis.setLowerBound(0)
      //xAxis.setUpperBound(100)
      xAxis.setLabel("Time")
      val yAxis = new NumberAxis()
      //yAxis.setAutoRanging(true)
      //yAxis.setLowerBound(-100)
      //xAxis.setUpperBound(100)
          
      lineChart = new LineChart[String, Number](xAxis, yAxis)
      lineChart.setTitle("Profit Monitoring")
      lineChart.setCreateSymbols(false)
      lineChart.setLegendVisible(false)

      //val root = new VBox()
      //root.getChildren.add(lineChart)
      scene = new Scene(lineChart, 1200, 900)
      scene.getStylesheets.add(cssUrl)
          
      fxPanel.setScene(scene)
      frame.pack
      frame.setVisible(true)
      println("GUI inited")
    }
  }
  
  private def startNewRound(param: Param) {
    idToSeries.clear
    runInFXThread {
      lineChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
      lineChart.setTitle("Profit Monitoring - " + param)
    }
  }
  
  private def updateData(data: ReportData) {
    // should run in FX application thread
    runInFXThread {
      val id = data.name + data.id
      val series = idToSeries.getOrElse(id, {
          val x = createSeries(data.name + "-" + data.id)
          idToSeries += (id -> x)
          x
        }
      )
      df.format(new Date(data.time))
      series.getData.add(new XYChart.Data(df.format(new Date(data.time)), data.value))
    }
  }
  
  private def createSeries(name: String): XYChart.Series[String, Number] = {
    val series = new XYChart.Series[String, Number]()
    series.setName(name)
    lineChart.getData.add(series)
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
      val dot = name.lastIndexOf(".")
      val ext = if (dot >= 0) {
        name.substring(dot + 1)
      } else {
        "jpg"
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
