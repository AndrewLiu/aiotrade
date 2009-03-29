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
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.aiotrade.platform.core.sec.Sec;

/**
 *
 * @author Caoyuan Deng
 */
public class Account {
    
    private String description;
    private Currency currency;
    private double initialBalance;
    private double currentBalance;
    
    private Commission commission;
    
    private List<Order> heldOrders = new ArrayList<Order>();
    private List<Transaction> transactions = new ArrayList<Transaction>();
    
    private Map<Sec, Position> secMapPosition = new HashMap<Sec, Position>();
    
    public Commission getCommission() {
        return commission;
    }
    
    public void setCommission(Commission commission) {
        this.commission = commission;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(Currency currency) {
        this.currency = currency;
        
    }
    
    public double getInitialBalance() {
        return initialBalance;
    }
    
    public void setInitialBalance(double initbalance) {
        this.initialBalance = initbalance;
        
    }
    
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        
        /** adjust position according to transaction */
        adjustPosition(transaction);
    }
    
    private void adjustPosition(Transaction transaction) {
        Sec sofic = transaction.getSofic();
        Position position = secMapPosition.get(sofic);
        
        if (position == null) {
            position = new Position(this, transaction);
            secMapPosition.put(sofic, position);
        } else {
            position.add(transaction);
            
            /** position may will be closed after this transaction */
            if (position.isClosed()) {
                secMapPosition.remove(sofic);
            }
        }
    }
    
    public void placeOrder(Order order) {
        heldOrders.add(order);
    }
    
    public void removeOrder(Order order) {
        heldOrders.remove(order);
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public Map<Sec, Position> getSecMapPosition() {
        return secMapPosition;
    }
    
    public int getOpenedPosition(Sec sofic) {
        int result = 0;
        
        for (Sec _sofic : secMapPosition.keySet()) {
            if (_sofic == sofic) {
                Position position = secMapPosition.get(_sofic);
                result += position.getCurrentQuantity();
            }
        }
        
        return result;
    }
    
    public double getBalance(Date date) {
        double result = initialBalance;
        
        for (Transaction transaction : getTransactions()) {
            if (transaction.getDate().before(date)) {
                result += transaction.getAmount();
            }
        }
        
        return result;
    }
    
    public double getAvailableCash() {
        double deposit = 0;
        for (Order order : heldOrders) {
            deposit += order.getAmount();
        }
        
        return getBalance(new Date()) - deposit;
    }
    
}
