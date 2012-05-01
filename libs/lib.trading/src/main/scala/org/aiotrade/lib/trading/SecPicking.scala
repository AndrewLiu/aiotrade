package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.SectorSec
import org.aiotrade.lib.util.ValidTime
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable

case class SecPickingEvent(secValidTime: ValidTime[Sec], side: Side)

/**
 * 
 * @author Caoyuan Deng
 */
class SecPicking extends Publisher {
  private var prevTime = 0L
  private val validTimes = new ArrayList[ValidTime[Sec]]
  val secToValidTimes = new mutable.HashMap[Sec, List[ValidTime[Sec]]]()
  
  private def addToMap(secValidTime: ValidTime[Sec]) {
    secToValidTimes(secValidTime.ref) = secValidTime :: secToValidTimes.getOrElse(secValidTime.ref, Nil)
  }

  private def removeFromMap(secValidTime: ValidTime[Sec]) {
    secToValidTimes.getOrElse(secValidTime.ref, Nil) filter (_ != secValidTime) match {
      case Nil => secToValidTimes -= secValidTime.ref
      case xs => secToValidTimes(secValidTime.ref) = xs
    }
  }
  
  def allSecs = secToValidTimes.keys
  
  def go(time: Long) {
    var i = 0
    while (i < validTimes.length) {
      val validTime = validTimes(i)
      if (validTime.isValid(time) && !validTime.isValid(prevTime)) {
        publish(SecPickingEvent(validTime, Side.EnterPicking))
      } else if (!validTime.isValid(time) && validTime.isValid(prevTime)) {
        publish(SecPickingEvent(validTime, Side.ExitPicking))
      }
      i += 1
    }
    prevTime = time
  }
  
  def +(secValidTime: ValidTime[Sec]) {
    validTimes += secValidTime
    addToMap(secValidTime)
  }
  
  def +(sec: Sec, fromTime: Long, toTime: Long) {
    this.+(ValidTime(sec, fromTime, toTime))
  }
  
  def -(secValidTime: ValidTime[Sec]) {
    validTimes -= secValidTime
    removeFromMap(secValidTime)
  }
  
  def -(sec: Sec, fromTime: Long, toTime: Long) {
    this.-(ValidTime(sec, fromTime, toTime))
  }
  
  def +=(secValidTime: ValidTime[Sec]): SecPicking = {
    this.+(secValidTime)
    this
  }

  def +=(sec: Sec, fromTime: Long, toTime: Long): SecPicking = {
    this.+(sec, fromTime, toTime)
    this
  }

  def -=(secValidTime: ValidTime[Sec]): SecPicking = {
    this.-(secValidTime)
    this
  }

  def -=(sec: Sec, fromTime: Long, toTime: Long): SecPicking = {
    this.-(sec, fromTime, toTime)
    this
  }

  def ++(secValidTimes: Seq[ValidTime[Sec]]) {
    this.validTimes ++= secValidTimes
    secValidTimes foreach addToMap
  }
  
  def ++=(secValidTimes: Seq[ValidTime[Sec]]): SecPicking = {
    this.++(secValidTimes)
    this
  }
  
  def --(secValidTimes: Seq[ValidTime[Sec]]) {
    this.validTimes --= secValidTimes
    secValidTimes foreach removeFromMap
  }
  
  def --=(secValidTimes: Seq[ValidTime[Sec]]): SecPicking = {
    this.--(secValidTimes)
    this
  }
  
  def isValid(sec: Sec, time: Long): Boolean = {
    secToValidTimes.get(sec) match {
      case Some(xs) => xs.exists(_.isValid(time))
      case None => false
    }
  }

  def iterator(time: Long): Iterator[Sec] = new IteratorAtTime(time)
  
  /**
   * Do (block) for each valid sec
   */
  def foreach(time: Long)(block: Sec => Unit) {
    val itr = iterator(time)
    while (itr.hasNext) {
      val sec = itr.next
      block(sec)
    }
  }

  def foldLeft[T](time: Long)(block: (Sec, T) => T)(result: T) = {
    var acc = result
    val itr = iterator(time)
    while (itr.hasNext) {
      val sec = itr.next
      acc = block(sec, acc)
    }
    acc
  }
  
  final class IteratorAtTime(time: Long) extends Iterator[Sec] {
    private var index = 0
      
    def hasNext = {
      while (index < validTimes.length && validTimes(index).isValid(time)) {
        index += 1
      }
      index < validTimes.length
    }
      
    def next = {
      if (hasNext) {
        val sec = validTimes(index).ref
        index += 1
        sec
      } else {
        null
      }
    }
  }
}

object SecPicking {
  val a = new SecPicking()
  val b = a += (new Sec, 1, 1)
  
  
  /**
   * @param The sector<->sec relation table records. They should belongs to the same sector
   */
  def toSecPicking(sectorSecs: Seq[SectorSec]): SecPicking = {
    val stockPicking = new SecPicking()
    for (sectorSec <- sectorSecs if sectorSec.sec ne null) {
      stockPicking += sectorSec.toSecValidTime
    }
    stockPicking
  }
}
