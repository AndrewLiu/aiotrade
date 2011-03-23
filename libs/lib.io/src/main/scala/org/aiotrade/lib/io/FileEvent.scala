package org.aiotrade.lib.io

import java.io.File

abstract class FileEvent(file: File, lastModified: Long) extends scala.swing.event.Event
case class FileAdded(file: File, lastModified: Long) extends FileEvent(file, lastModified)
case class FileDeleted(file: File, lastModified: Long) extends FileEvent(file, lastModified)
case class FileModified(file: File, lastModified: Long) extends FileEvent(file, lastModified)