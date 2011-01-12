package org.aiotrade.lib.io

import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.logging.{Logger, Level}
import scala.collection.mutable

object DirWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    //Test watching the single directory
    val task = new DirWatcher("/tmp/test01", "txt" ) {
      override protected def onChange(event: FileEvent) {
        println("task:----- " + event)
      }
      override protected def lastModified(file: File): Long = {
        val f = new java.io.BufferedReader(new java.io.FileReader(file))
        val timestamp = f.readLine.toLong
        f.close
        timestamp
      }

    }
    val timer = new Timer
    timer.schedule(task , new Date, 1000)

    //Test watching the two directories simutaneously
    val task2 = new DirWatcher("/tmp/test02", "/tmp/test03", ".txt") {
      override protected def onChange(event: FileEvent) {
        println("task2:----- " + event)
      }

      override protected def lastModified(file: File): Long = {
        val f = new java.io.BufferedReader(new java.io.FileReader(file))
        val timestamp = f.readLine.toLong
        f.close
        timestamp
      }
    }
    val timer2 = new Timer
    timer2.schedule(task2, new Date, 1000)
    
  }
}

/**
 * The DirWatcher watches two directories simutaneously.
 * If a file with the same file name emerges in the two directories,
 * only the latest one will be notified, the old one will be ignored.
 *
 * If only one path is provided for DirWatcher and the other path is null,
 * the Dirwatcher will only watch one directory and ignore the null path.
 *
 * @author Caoyuan Deng, Guibin Zhang
 */
@throws(classOf[IOException])
abstract class DirWatcher(path01: File, path02: File, filter: FileFilter, includingExistingFiles: Boolean = false) extends TimerTask {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val _fileNameToLastModified = new WatcherMap

  protected val NOT_SURE = Long.MinValue

  log.log(Level.INFO, "DirWatcher watching on " + path01 + " and " + path02)
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
  def init {
    for (path <- List(path01, path02) if path != null) {
      path listFiles filter match {
        case null => log.log(Level.SEVERE, path + " is not a valid directory or I/O error occurs.")
        case existed =>
          var i = 0
          while (i < existed.length) {
            val file = existed(i)
            lastModified(file) match {
              case NOT_SURE =>
              case x => _fileNameToLastModified.put(file, x)
            }
            i += 1
          }
      }
    }

    if (includingExistingFiles) {
      _fileNameToLastModified.exportToFiles.sortWith(_.compareTo(_) < 0) foreach {x => onChange(FileAdded(x))}
    }
  }

  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    val startTimestamp = System.currentTimeMillis
    val fileNameToLastModified = new WatcherMap
    
    //Scan both directories to get current files.
    for (path <- List(path01, path02) if path != null) {
      try {
        val files = {
          path listFiles filter match {
            case null =>
              log.log(Level.SEVERE, path + " is not a valid directory or I/O error occurs.")
              Array[File]()
            case xs => xs
          }
        }
          
        var i = 0
        while (i < files.length) {
          val file = files(i)
          lastModified(file) match {
            case NOT_SURE =>
            case x => fileNameToLastModified.put(file, x)
          }
          i += 1
        }
      } catch {
        case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex)
      }
    }

    // It is to Guarantee that the name strings in the resulting array will appear in alphabetical order.
    val fileNames = fileNameToLastModified.exportToFileNames.sortWith(_.compareTo(_) < 0)
    val checkedFiles = mutable.Set[String]()

    var i = 0
    while (i < fileNames.length) {
      val fileName = fileNames(i)
      checkedFiles += fileName

      _fileNameToLastModified.get(fileName) match {
        case None =>
          // new file
          val file = fileNameToLastModified.fileOf(fileName).get
          if (file.canRead) {
            _fileNameToLastModified.put(file, fileNameToLastModified.get(file).get)
            onChange(FileAdded(file))
          }
        case Some(lastModified) if lastModified < fileNameToLastModified.get(fileName).get =>
          // modified file
          val file = fileNameToLastModified.fileOf(fileName).get
          if (file.canRead) {
            _fileNameToLastModified.put(file, fileNameToLastModified.get(file).get)
            onChange(FileModified(file))
          }
        case x => //Ingore the old one and current one, only care the newer one.
      }
      
      i += 1
    }

    // deleted files
    val deletedfiles = (mutable.Set() ++ _fileNameToLastModified.exportToFileNames.clone) -- checkedFiles
    deletedfiles foreach {fileName =>
      val file = _fileNameToLastModified.fileOf(fileName).get
      _fileNameToLastModified remove file
      onChange(FileDeleted(file))
    }
    
    val duration = System.currentTimeMillis - startTimestamp
    if (duration > 2000) {
      log.log(Level.WARNING, "Scaning " + path01 + " and " + path02 + " costs " + duration / 1000F + " seconds")
    }
  }

  /**
   * Override it if you want sync processing
   */
  protected def onChange(event: FileEvent) {}

  /**
   * Override it if you want to get the timestamp by other way,
   * such as by some the content of the file
   *
   * @return last modified time or NOT_SURE
   */
  protected def lastModified(file: File): Long = {
    file.lastModified
  }

  
  protected class WatcherMap {
    private val fileNameToLastModified = mutable.Map[String, (File, Long)]()

    /**
     * Filter out the duplicated file whose file name is same,
     * only keep the latest one according to lastModified()
     */
    def put(file: File, lastModified: Long): Boolean = {
      fileNameToLastModified.get(file.getName) match {
        case Some((f, timestamp)) =>
          //Only keep the latest one
          if (timestamp < lastModified){
            fileNameToLastModified(file.getName) = (file, lastModified)
            true
          } else {
            false
          }
        case None =>
          fileNameToLastModified(file.getName) = (file, lastModified)
          true
      }
    }

    def get(file: File): Option[Long] = {
      fileNameToLastModified.get(file.getName) match {
        case Some((f, timestamp)) => Some(timestamp)
        case None => None
      }
    }

    def get(fileName: String): Option[Long] = {
      fileNameToLastModified.get(fileName) match {
        case Some((f, timestamp)) => Some(timestamp)
        case None => None
      }
    }

    def fileOf(fileName: String) : Option[File] = {
      fileNameToLastModified.get(fileName) match {
        case Some((f, timestamp)) => Some(f)
        case None => None
      }
    }

    def remove(file: File) {
      fileNameToLastModified.get(file.getName) match {
        case Some((f, timestamp)) => fileNameToLastModified -= file.getName
        case _ =>
      }
    }

    def exportToFiles: Array[File] = {
      fileNameToLastModified.values.map(_._1).toArray
    }

    def exportToFileNames: Array[String] = {
      fileNameToLastModified.keys.toArray
    }

    def clear {
      fileNameToLastModified.clear
    }

    def getAll = fileNameToLastModified
  }
}


class DefaultWatcherFilter(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = {
    filter == "" || file.getName.endsWith(filter)
  }
}