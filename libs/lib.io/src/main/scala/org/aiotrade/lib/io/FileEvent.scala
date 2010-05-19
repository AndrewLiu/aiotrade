package org.aiotrade.lib.io

import java.io.File

abstract class FileEvent(file: File) extends scala.swing.event.Event
case class FileAdded(file: File) extends FileEvent(file)
case class FileDeleted(file: File) extends FileEvent(file)
case class FileModified(file: File) extends FileEvent(file)