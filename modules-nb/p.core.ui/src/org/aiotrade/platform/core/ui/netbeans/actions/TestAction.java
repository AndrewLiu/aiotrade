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
package org.aiotrade.platform.core.ui.netbeans.actions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
public class TestAction extends CallableSystemAction {
    
    /** Creates a new instance
     */
    public TestAction() {
    }
    
    
    public void performAction() {
        try {
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    testHsqldb();
                }
            });
        } catch (Exception e) {
        }
        
    }
    
    private void testHsqldb() {
        String userDir = System.getProperty("netbeans.user");
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        
        try {
            Connection conn = DriverManager.getConnection("jdbc:hsqldb:file:" + userDir + "/db/aiotrade", "sa", "");
            Statement stmt = conn.createStatement();
            ResultSet rs;
            String sql;
            
            boolean exist = false;
            try {
                rs = stmt.executeQuery("select count(1) from quote");
                exist = true;
            } catch (SQLException ex) {
                exist = false;
            }
            
            if (!exist) {
                sql =   "CREATE CACHED TABLE quote (" +
                        "quote_id IDENTITY not null, " +
                        "symbol CHAR(12) not null, " +
                        "longtime BIGINT not null, " +
                        "open FLOAT, " +
                        "high FLOAT, " +
                        "low FLOAT, " +
                        "close FLOAT, " +
                        "close_adj FLOAT, " +
                        "volume INTEGER, " +
                        "amount FLOAT, " +
                        "CONSTRAINT pk_quote PRIMARY KEY (quote_id)" +
                        ")";
                
                stmt.executeUpdate(sql);
                
                sql = "CREATE INDEX idx_symbol ON quote (symbol)";
                stmt.executeUpdate(sql);
                
                sql = "CREATE INDEX idx_longtime ON quote (longtime)";
                stmt.executeUpdate(sql);
                
                conn.commit();
            }
            
            sql = "insert into quote (symbol, longtime, open) values ('yhoo', 9223372036854775807, 12)";
            stmt.executeUpdate(sql);
            
            conn.commit();
            
            rs = stmt.executeQuery("select * from quote");
            
            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    System.out.println(i + ": " + rs.getString(i));
                }
            }
            
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
    }
    
    public String getName() {
        return "Test ...";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}




