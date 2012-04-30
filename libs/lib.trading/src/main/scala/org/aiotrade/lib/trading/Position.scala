package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec

class Position private (private var _time: Long, private var _sec: Sec, private var _quantity: Double, private var _price: Double) {
  def this() = this(Long.MinValue, null, Double.NaN, Double.NaN) /* for serializable */  

  private var _subPositions: ArrayList[Position] = null
  private var _currentPrice = _price
  private var _maxProfitRatio = 0.0
  
  def subPositions: Array[Position] = if (_subPositions == null) Array() else _subPositions.toArray
  
  def sec = _sec
  def sec_=(sec: Sec) {
    _sec = sec
    if (_subPositions != null) {
      var i = 0
      while (i < _subPositions.length) {
        val pos = _subPositions(i)
        pos._sec = sec
        i += 1
      }
    
    }
  }
  
  def time = _time
  def time_=(time: Long) = {
    _time = time
  }
  
  def quantity = _quantity
  def quantity_=(quantity: Double) {
    _quantity = quantity
  }
  
  def price = _price
  def price_=(price: Double) {
    _price = price
  }
  
  def equity = _currentPrice * quantity
  
  def currentPrice = _currentPrice
  
  def update(currentPrice: Double) {
    if (!currentPrice.isNaN) {
      _currentPrice = currentPrice
      _maxProfitRatio = math.max(_maxProfitRatio, profitRatio)
    }
  }
  
  def add(time: Long, quantity: Double, price: Double) {
    _subPositions = if (_subPositions == null) new ArrayList[Position]() else _subPositions
    _subPositions += new Position(time, sec, quantity, price)
    
    if (math.signum(quantity) == math.signum(_quantity) || math.signum(_quantity) == 0.0) {
      val total = _quantity * _price + quantity * price
      _quantity += quantity
      _price = total / _quantity
    } else {
      _quantity += quantity
      if (math.signum(quantity) == math.signum(_quantity)) {
        _price = price
      }
    }
  }
  
  def isLong: Boolean = _quantity > 0
  def isShort: Boolean = _quantity < 0
  
  /**
   * @todo, consider expense
   */
  def profitRatio = if (_quantity == 0) 0 else if (isLong) (_currentPrice - _price) / _price else (_price - _currentPrice) / _price
  def maxProfitRatio = _maxProfitRatio
}

object Position {
  def apply(time: Long, sec: Sec, quantity: Double, price: Double) = new Position(time, sec, quantity, price)
  def apply() = new Position(Long.MinValue, null, Double.NaN, Double.NaN)
}

trait PositionEvent {
  def account: Account
  def position: Position
}
case class PositionOpened(val account: Account, val position: Position) extends PositionEvent
case class PositionClosed(val account: Account, val position: Position) extends PositionEvent
case class PositionChanged(val account: Account, val position: Position) extends PositionEvent
