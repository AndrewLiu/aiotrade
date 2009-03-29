/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.platform.modules.portfolio.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.aiotrade.platform.core.analysis.indicator.Direction;
import org.aiotrade.platform.core.sec.Sec;


/**
 *
 * @author Caoyuan Deng
 */
public class Position {
    private Account account;
    
    private Direction direction = Direction.Long;    // Long or Short
    private Sec sofic;
    private int currentQuantity;
    private int initialQuantity;    // Quantity when position was opened
    private double openPrice;        // Price when position is opened
    private double closePrice;       // Price when position is closed
    private double stopPrice;        // Stop price
    private Date openDate;
    private Date closeDate;
    
    private List<Transaction> transactions = new ArrayList<Transaction>();
    
    public Position(Account account, Transaction transaction) {
        init(account, transaction.getDate(), transaction.getSofic(), transaction.getQuantity(), transaction.getAmount(), Direction.Long);
    }
    
    private void init(Account account, Date date, Sec sofic, int quantity, double amount, Direction direction) {
        this.account = account;
        this.sofic = sofic;
        this.openDate = date;
        this.initialQuantity = quantity;
        this.currentQuantity = quantity;
        this.openPrice = amount / Math.abs(quantity);
        this.direction = direction;
    }
    
    public Sec getSofic() {
        return sofic;
    }
    
    public void add(Transaction transaction) {
        transactions.add(transaction);
        
        int quantity = transaction.getQuantity();
        double amount = transaction.getAmount();
        
        if (quantity >= 0) {
            /** buy */
            double totalBuy = currentQuantity * openPrice;
            totalBuy += amount;
            
            currentQuantity += quantity;
            openPrice = totalBuy / currentQuantity;
        } else {
            /** sell */
            double totalSell = (initialQuantity - currentQuantity) * closePrice;
            totalSell += amount;
            
            currentQuantity += quantity;
            closePrice = totalSell / (initialQuantity - currentQuantity);
        }
        
        if (isClosed()) {
            closeDate = transaction.getDate();
        }
    }
    
    public boolean isClosed() {
        return (currentQuantity == 0) ? true : false;
    }
    
    public double getStopPrice() {
        return stopPrice;
    }
    
    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }
    
    public int getCurrentQuantity() {
        return currentQuantity;
    }
    
    public double getValue() {
        return Math.abs(currentQuantity) * openPrice;
    }
    
    public double getMarketValue(float lastPrice) {
        double result = Math.abs(currentQuantity) * lastPrice;
        
        double expenses = account.getCommission().computeExpenses(result);
        result -= expenses;
        
        return result;
    }
}
