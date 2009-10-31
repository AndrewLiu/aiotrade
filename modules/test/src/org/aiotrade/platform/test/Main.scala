/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.applet;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.ref.Reference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.aiotrade.platform.core.analysis.chartview.AnalysisChartViewContainer;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooQuoteServer;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooTickerServer;


/**
 *
 * @author Caoyuan Deng
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        int w = 600;
        int h = 500;

        frame.setSize(w, h);

        Container pane = frame.getContentPane();
        pane.setBackground(Color.WHITE);
        
        try {
            Util.setBaseUrl(new URL("http://mms2009.vicp.net"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        String symbol = "BP.L";

        Collection<Reference<AnalysisChartViewContainer>> containers = new Util().drawChart(pane, w, h, symbol, "", "", YahooQuoteServer.class, YahooTickerServer.class);

        for (Reference<AnalysisChartViewContainer> viewContainer : containers) {
            viewContainer.get().setPreferredSize(new Dimension(w, h));
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        frame.setVisible(true);
    }
}
