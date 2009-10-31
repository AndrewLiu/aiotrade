/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.applet;

import com.nyapc.aiotrade.dataserver.ApcQuoteServer;
import com.nyapc.aiotrade.dataserver.ApcTickerServer;
import java.awt.Container;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.aiotrade.lib.util.swing.plaf.HighContrastLAF;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooQuoteServer;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooTickerServer;

/**
 *
 * @author Caoyuan Deng
 */
public class MainApplet extends JApplet {

    private static String testUrl = "http://tt1/";
    private Util util;

    /**
     * Initialization method that will be called after the applet is loaded
     * into the browser.
     */
    @Override
    public void init() {
        System.out.println("Init AIOTrade applet.");
        try {
            //UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.setLookAndFeel(new HighContrastLAF());
        } catch (UnsupportedLookAndFeelException ex) {
        }

        String dataSource = getParameter("dataSource");
        if (dataSource == null) {
            dataSource = "nypac";
        }

        String symbol = getParameter("symbol");

        String type = getParameter("type");
        if (type == null) {
            type = "003001";
        }

        String sname = getParameter("sname");
        if (sname == null) {
            sname = "";
        }
        System.out.println("name: " + sname);

        Container pane = this.getContentPane();

        int w = pane.getWidth();
        int h = pane.getHeight();

        URL baseUrl = getCodeBase();
        if (baseUrl.getProtocol().equals("file")) {
            try {
                baseUrl = new URL(testUrl);
            } catch (MalformedURLException ex) {
            }
        }

        Util.setBaseUrl(baseUrl);

        util = new Util();
        if (dataSource.equals("yahoo")) {
            util.drawChart(pane, w, h, symbol == null ? "600690.SS" : symbol, type, sname, YahooQuoteServer.class, YahooTickerServer.class);
        } else {
            ApcQuoteServer.setBaseUrl(baseUrl);
            ApcTickerServer.setBaseUrl(baseUrl);
            util.drawChart(pane, w, h, symbol == null ? "600690.SH" : symbol, type, sname, ApcQuoteServer.class, ApcTickerServer.class);
        }
    }

    /** will be called when back to this page */
    @Override
    public void start() {
        super.start();
        System.out.println("Start AIOTrade applet.");
    }

    /** will be called when leave this page */
    @Override
    public void stop() {
        super.stop();
        System.out.println("Stop AIOTRade applet.");
    }

    @Override
    public void destroy() {
        util.releaseAll();
        super.destroy();
        System.out.println("Destroy AIOTRade applet.");
    }
    // TODO overwrite start(), stop() and destroy() methods
}
