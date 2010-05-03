package org.aiotrade.lib.io

import java.io.File
import java.util.Date
import java.util.Timer
import java.util.TimerTask

object FileWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new FileWatcher(new File("temp.txt")) {
      protected def onChange(event: Event) {
        println(event)
      }
    }

    val timer = new Timer
    timer.schedule(task, new Date, 1000)
  }
}

abstract class FileWatcher(file: File) extends TimerTask {
  private var timeStamp: Long = file.lastModified

  final def run {
    if (file.exists) {
      val timeStamp = file.lastModified
      if (this.timeStamp != timeStamp) {
        this.timeStamp = timeStamp
        onChange(Modified(file))
      }
    } else {
      onChange(Deleted(file))
    }
  }

  protected def onChange(event: Event): Unit
}