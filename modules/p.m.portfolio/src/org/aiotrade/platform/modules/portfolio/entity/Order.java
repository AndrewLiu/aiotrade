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
import org.aiotrade.platform.core.sec.Sec;

/**
 *
 * @author Caoyuan Deng
 */
public class Order {
    
    private Account account;
    
    private Sec sofic;
    
    private Date date;
    private Date Expiration;
    
    private double price;
    private double price2;
    
    private int quantity;
    
    private double amount;
    
    /** an order may be striken via more than one transactions */
    private List<Transaction> transactions = new ArrayList<Transaction>();
    
    public Order(Account account, Sec sofic, Date date, double price, int quantity) {
        this.account = account;
        this.sofic = sofic;
        this.date = date;
        this.quantity = quantity;
        this.price = price;
        
        this.amount = this.price * this.quantity;
    }
    
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
    
    public void strick(Transaction transaction) {
        account.addTransaction(transaction);
        
        this.transactions.add(transaction);
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public Sec getSofic() {
        return sofic;
    }
    
    public void setSofic(Sec sofic) {
        this.sofic = sofic;
    }
    
    public Date getDate() {
        return date;
    }
    
    public Date getExpiration() {
        return Expiration;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public double getPrice() {
        return this.price;
    }
    
    public int getQuantity() {
        return this.quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public double getExpenses() {
        double result = 0;
        
        for (Transaction transaction : getTransactions()) {
            result += transaction.getExpenses();
        }
        
        return result;
    }
    
    public double getAmount() {
        return amount;
    }

}
