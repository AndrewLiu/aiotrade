package org.aiotrade.lib.util.file

import java.io.File
import java.util.Date
import java.util.Timer
import java.util.TimerTask

object FileWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new FileWatcher(new File("temp.txt")) {
      protected def onChange(file: File ) {
        println("File "+ file.getName + " have change !")
      }
    }

    val timer = new Timer
    timer.schedule( task , new Date, 1000)
  }
}

abstract class FileWatcher(file: File) extends TimerTask {
  private var timeStamp: Long = file.lastModified

  final def run {
    val timeStamp = file.lastModified

    if( this.timeStamp != timeStamp ) {
      this.timeStamp = timeStamp
      onChange(file)
    }
  }

  protected def onChange(file: File): Unit
}