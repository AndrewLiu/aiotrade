package org.aiotrade.lib.io

import java.io.File
import java.util.Date
import java.util.Timer
import java.util.TimerTask

object FileWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new FileWatcher(new File("temp.txt")) {
      override protected def onChange(event: FileEvent) {
        println(event)
      }
    }

    val timer = new Timer
    timer.schedule(task, new Date, 1000)
  }
}

class FileWatcher(file: File) extends TimerTask with scala.swing.Publisher {
  private var timeStamp: Long = file.lastModified

  final def run {
    apply()
  }

  final def apply() {
    if (file.exists) {
      val timeStamp = file.lastModified
      if (this.timeStamp != timeStamp) {
        this.timeStamp = timeStamp
        onChange(FileModified(file))
      }
    } else {
      onChange(FileDeleted(file))
    }
  }

  /**
   * Override it if you want sync processing
   */
  protected def onChange(event: FileEvent) {
    publish(event)
  }
}