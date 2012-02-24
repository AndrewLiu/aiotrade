package org.aiotrade.lib.trading

import org.aiotrade.lib.securities.model.Sec

class Position(val sec: Sec, private var _quantity: Double, private var _price: Double) {
  def quantity = _quantity
  def price = _price
  
  def add(quantity: Double, price: Double) {
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
}

trait PositionEvent {
  def account: Account
  def position: Position
}
case class PositionOpened(val account: Account, val position: Position) extends PositionEvent
case class PositionClosed(val account: Account, val position: Position) extends PositionEvent
case class PositionChanged(val account: Account, val position: Position) extends PositionEvent
