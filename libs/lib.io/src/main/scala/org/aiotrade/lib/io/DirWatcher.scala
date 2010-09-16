package org.aiotrade.lib.io

import java.io.File
import java.io.FileFilter
import java.io.IOException
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

@throws(classOf[IOException])
abstract class DirWatcher(path: File, filter: FileFilter, includingExistingFiles: Boolean = false) extends TimerTask {
  private val fileToLastModified = new HashMap[File, Long]

  path.listFiles(filter) match {
    case null =>
    case existed => 
      var i = 0
      while (i < existed.length) {
        val x = existed(i)
        fileToLastModified.put(x, lastModified(x))
        i += 1
      }
      if (includingExistingFiles) {
        existed.sortWith(_.compareTo(_) < 0) foreach {x => onChange(FileAdded(x))}
      }
  }


  def this(path: String, filter: FileFilter) = this(new File(path), filter)
  def this(path: String, filterStr: String) = this(path, new DirWatcherFilter(filterStr))
  def this(path: String) = this(path, "")

  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    // It is to Guarantee that the name strings in the resulting array will appear in alphabetical order.
    val files = path listFiles filter match {
      case null => return
      case x => x sortWith(_.compareTo(_) < 0)
    }

    val checkedFiles = new HashSet[File]

    var i = 0
    while (i < files.length) {
      val file = files(i)
      checkedFiles += file
      
      fileToLastModified.get(file) match {
        case None =>
          // new file
          if(file.canRead){
            fileToLastModified.put(file, lastModified(file))
            onChange(FileAdded(file))
          }
        case Some(last) if last != lastModified(file) =>
          // modified file
          if(file.canRead){
            fileToLastModified.put(file, lastModified(file))
            onChange(FileModified(file))
          }
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
  protected def onChange(event: FileEvent){}

  /**
   * Override it if you want to get the timestamp by other way,
   * such as by some the content of the file
   */
  protected def lastModified(file: File): Long = {
    file.lastModified
  }
}

class DirWatcherFilter(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = 
    if (filter != "") file.getName.endsWith(filter) else true
}