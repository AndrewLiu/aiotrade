package org.aiotrade.lib.backtest

import org.aiotrade.lib.trading.Account

class MoneyManagementRule(account: Account) {
  def maxMoneyPerSec: Double = Double.PositiveInfinity
  def maxMoneyProportionPerSec: Double = 1.0
  def maxNumSecsPerPortfolio: Int = 5
  def maxNumPortfolioPerAccount: Int = 1
  
}
