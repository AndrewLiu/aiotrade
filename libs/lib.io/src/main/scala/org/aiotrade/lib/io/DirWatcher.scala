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
    timer.schedule(task , new Date, 1000)
  }
}

class DirWatcher(path: File, filter: FileFilter) extends TimerTask {
  private val fileToLastModified = new HashMap ++ (path listFiles filter map (x => x -> x.lastModified))

  def this(path: String, filter: FileFilter) = this(new File(path), filter)
  def this(path: String, filterStr: String) = this(path, new DirWatcherFilter(filterStr))
  def this(path: String) = this(path, "")

  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    val files = path listFiles filter
    val checkedFiles = new HashSet[File]

    var i = 0
    while (i < files.length) {
      val file = files(i)
      checkedFiles += file
      
      fileToLastModified.get(file) match {
        case None =>
          // new file
          fileToLastModified.put(file, file.lastModified)
          onChange(FileAdded(file))
        case Some(lastModified) if lastModified != file.lastModified =>
          // modified file
          fileToLastModified.put(file, file.lastModified)
          onChange(FileModified(file))
        case _ =>
      }
      
      i += 1
    }

    // deleted files
    val deletedfiles = fileToLastModified.clone.keySet -- checkedFiles
    deletedfiles foreach {file =>
      fileToLastModified -= file
      onChange(FileDeleted(file))
    }
  }

  /**
   * Override it if you want sync processing
   */
  protected def onChange(event: FileEvent)
}

class DirWatcherFilter(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = 
    if (filter != "") file.getName.endsWith(filter) else true
}