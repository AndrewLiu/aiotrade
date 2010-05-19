package org.aiotrade.lib.io

import java.io.File
import java.io.FileFilter
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

object DirWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new DirWatcher("temp", "txt" ) {
      override protected def onChange(event: FileEvent) {
        println(event)
      }
    }

    val timer = new Timer
    timer.schedule(task , new Date, 1000 )
  }
}

class DirWatcher(path: String, filter: String) extends TimerTask with scala.swing.Publisher {
  private val dfw = new DirFilterWatcher(filter)
  private val dirs = new HashMap ++ (new File(path) listFiles dfw map (x => x -> x.lastModified))

  def this(path: String) = this(path, "")

  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    val files = new File(path) listFiles dfw
    val checkedFiles = new HashSet[File]

    var i = 0
    while (i < files.length) {
      val file = files(i)
      checkedFiles += file
      
      dirs.get(file) match {
        case None =>
          // new file
          dirs += (file -> file.lastModified)
          onChange(FileAdded(file))
        case Some(lastModified) if lastModified != file.lastModified =>
          // modified file
          dirs += (file -> file.lastModified)
          onChange(FileModified(file))
        case _ =>
      }
      
      i += 1
    }

    // deleted files
    val deletedfiles = dirs.clone.keySet -- checkedFiles
    deletedfiles foreach {file =>
      dirs -= file
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

class DirFilterWatcher(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = 
    if (filter != "") file.getName.endsWith(filter) else true
}