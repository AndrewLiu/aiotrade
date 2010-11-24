package org.aiotrade.lib.io

import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.logging.{Logger, Level}
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

object DirWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new DirWatcher("/tmp/test01", "txt" ) {
      override protected def onChange(event: FileEvent) {
        println("task: " + event)
      }
    }

    val task2 = new DirWatcher("/tmp/test02", "/tmp/test03", "txt") {
      override protected def onChange(event: FileEvent) {
        println("task2: " + event)
      }
    }
    val timer = new Timer
    timer.schedule(task , new Date, 1000)
    timer.schedule(task2, new Date, 1000)
  }
}

/**
 * The DirWatcher watches two directories simutaneously.
 * If a file with the same file name emerges in the two directories,
 * only the first one will be notified, the other will be ignored.
 *
 * If only one path is provided for DirWatcher and the other path is null,
 * the Dirwatcher will only watch one directory and ignore the null path.
 *
 * @author Caoyuan Deng, Guibin Zhang
 */
@throws(classOf[IOException])
abstract class DirWatcher(path01: File, path02: File, filter: FileFilter, includingExistingFiles: Boolean = false) extends TimerTask {

  private val log = Logger.getLogger(this.getClass.getName)
  private val fileToLastModified = new WatcherMap

  log.log(Level.INFO, "Watching on " + path01 + " and " + path02)
  init

  def this(path01: String, path02: String, endWith: String) =
    this(new File(path01), new File(path02), new DefaultWatcherFilter(endWith), false)
  def this(path01: String, path02: String, filter: FileFilter) =
    this(new File(path01), new File(path02), filter, false)
  
  def this(path: File, filter: FileFilter, includingExistingFiles: Boolean) =
    this(path, null, filter, includingExistingFiles)
  def this(path: File, filter: FileFilter) = this(path, filter, false)
  def this(path: String, filter: FileFilter) = this(new File(path), filter)
  def this(path: String, endWith: String) = this(path, new DefaultWatcherFilter(endWith))
  def this(path: String) = this(path, "")


  /**
   * Processing the existing files in the watched directories
   */
  def init() = {
    List(path01, path02).foreach {
      path =>
      if(path != null) {
        path listFiles filter match {
          case null => log.log(Level.SEVERE, path + " is not a valid directory or I/O error occurs.")
          case existed =>
            var i = 0
            while (i < existed.length) {
              val x = existed(i)
              fileToLastModified.put(x, lastModified(x))
              i += 1
            }
        }
      }
    }

    if (includingExistingFiles) {
      fileToLastModified.export.sortWith(_.compareTo(_) < 0) foreach {x => onChange(FileAdded(x))}
    }
  }

  
  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    val $fileNameToLastModified = new WatcherMap

    //Scan both directories to get current files.
    List(path01, path02).foreach {
      path =>
      if(path != null){
        try{
          val files = {path listFiles filter match {
              case null =>
                log.log(Level.SEVERE, path + " is not a valid directory or I/O error occurs.")
                Array[File]()
              case x => x
            }
          }
          
          var i = 0
          while(i < files.length) {
            $fileNameToLastModified.put(files(i), lastModified(files(i)))
            i += 1
          }
        } catch{case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex)}
      }
    }

    // It is to Guarantee that the name strings in the resulting array will appear in alphabetical order.
    val files = $fileNameToLastModified.export.sortWith(_.compareTo(_) < 0)
    val checkedFiles = new HashSet[File]

    var i = 0
    while (i < files.length) {
      val file = files(i)
      checkedFiles += file

      fileToLastModified.get(file) match {
        case None =>
          // new file
          if (file.canRead) {
            fileToLastModified.put(file, $fileNameToLastModified.get(file).get)
            onChange(FileAdded(file))
          }
        case Some(last) if last != $fileNameToLastModified.get(file).get =>
          // modified file
          if (file.canRead) {
            fileToLastModified.put(file, $fileNameToLastModified.get(file).get)
            onChange(FileModified(file))
          }
        case _ =>
      }
      
      i += 1
    }

    // deleted files
    val deletedfiles = (new HashSet ++ fileToLastModified.export.clone) -- checkedFiles
    deletedfiles foreach {file =>
      fileToLastModified remove file
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


  
  private class WatcherMap{
    private val _fileNameToLastModified = new HashMap[String, (File, Long)]

    /**
     * Filter out the duplicated file whose file name is same,
     * only keep the latest one according to lastModified()
     */
    def put(_file: File, _lastModified: Long): Boolean = {
      _fileNameToLastModified.get(_file.getName) match {
        case Some((f, timestamp)) =>
          //Only keep the latest one
          if(timestamp < _lastModified){
            _fileNameToLastModified(_file.getName) = (_file, _lastModified)
            true
          }
          else {
            false
          }
        case None =>
          _fileNameToLastModified(_file.getName) = (_file, _lastModified)
          true
      }
    }

    def get(_file: File): Option[Long] = {
      _fileNameToLastModified.get(_file.getName) match {
        case Some((f, timestamp)) => Some(timestamp)
        case _ => None
      }
    }

    def remove(_file: File) {
      _fileNameToLastModified.get(_file.getName) match {
        case Some((f, timestamp)) => _fileNameToLastModified -= _file.getName
        case _ =>
      }
    }

    def export: Array[File] = {
      val result = new scala.collection.mutable.ArrayBuffer[File]
      _fileNameToLastModified.values.foreach(m => result + m._1)
      result.toArray
    }
  }
}


class DefaultWatcherFilter(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = 
    if (filter != "") file.getName.endsWith(filter) else true
}