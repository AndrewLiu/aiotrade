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
package org.aiotrade.platform.modules.dataserver.ib;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;

/**
 * 
 * @author Caoyuan Deng
 */
public class EWrapperAdapter implements EWrapper {
    
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute) {};
    public void tickSize( int tickerId, int field, int size) {};
    public void tickOptionComputation( int tickerId, int field, double impliedVolatility, double delta) {};
    public void orderStatus( int orderId, String status, int filled, int remaining,
            double avgFillPrice, int permId, int parentId, double lastFillPrice,
            int clientId) {};
    public void openOrder( int orderId, Contract contract, Order order) {};
    public void error( String str) {};
    public void connectionClosed() {};
    public void updateAccountValue(String key, String value, String currency, String accountName) {};
    public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue,
            double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {};
    public void updateAccountTime(String timeStamp) {};
    public void nextValidId( int orderId) {};
    public void contractDetails(ContractDetails contractDetails) {};
    public void bondContractDetails(ContractDetails contractDetails) {};
    public void execDetails( int orderId, Contract contract, Execution execution) {};
    public void error(int id, int errorCode, String errorMsg) {};
    public void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size) {};
    public void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation, int side, double price, int size) {};
    public void updateNewsBulletin( int msgId, int msgType, String message, String origExchange) {};
    public void managedAccounts( String accountsList) {};
    public void receiveFA(int faDataType, String xml) {};
    public void historicalData(int reqId, String date, double open, double high, double low,
                      double close, int volume, double WAP, boolean hasGaps) {};
    public void scannerParameters(String xml) {};
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection) {};
}

