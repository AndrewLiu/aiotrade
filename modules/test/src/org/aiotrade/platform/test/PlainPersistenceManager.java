/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.applet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.aiotrade.platform.core.PersistenceManager;
import org.aiotrade.platform.core.dataserver.QuoteServer;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Quote;
import org.aiotrade.platform.core.sec.QuotePool;
import org.aiotrade.platform.core.sec.TickerPool;
import org.aiotrade.platform.modules.indicator.basic.ARBRIndicator;
import org.aiotrade.platform.modules.indicator.basic.BIASIndicator;
import org.aiotrade.platform.modules.indicator.basic.BOLLIndicator;
import org.aiotrade.platform.modules.indicator.basic.CCIIndicator;
import org.aiotrade.platform.modules.indicator.basic.DMIIndicator;
import org.aiotrade.platform.modules.indicator.basic.EMAIndicator;
import org.aiotrade.platform.modules.indicator.basic.GMMAIndicator;
import org.aiotrade.platform.modules.indicator.basic.HVDIndicator;
import org.aiotrade.platform.modules.indicator.basic.KDIndicator;
import org.aiotrade.platform.modules.indicator.basic.MACDIndicator;
import org.aiotrade.platform.modules.indicator.basic.MAIndicator;
import org.aiotrade.platform.modules.indicator.basic.MFIIndicator;
import org.aiotrade.platform.modules.indicator.basic.MTMIndicator;
import org.aiotrade.platform.modules.indicator.basic.OBVIndicator;
import org.aiotrade.platform.modules.indicator.basic.ROCIndicator;
import org.aiotrade.platform.modules.indicator.basic.RSIIndicator;
import org.aiotrade.platform.modules.indicator.basic.SARIndicator;
import org.aiotrade.platform.modules.indicator.basic.WMSIndicator;
import org.aiotrade.platform.modules.indicator.basic.ZIGZAGFAIndicator;
import org.aiotrade.platform.modules.indicator.basic.ZIGZAGIndicator;
import com.nyapc.aiotrade.dataserver.ApcQuoteServer;
import com.nyapc.aiotrade.dataserver.ApcTickerServer;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents;
import org.aiotrade.lib.indicator.VOLIndicator;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooQuoteServer;
import org.aiotrade.platform.modules.dataserver.yahoo.YahooTickerServer;

/**
 *
 * @author Caoyuan Deng
 */
public class PlainPersistenceManager implements PersistenceManager.I {

    private List<QuoteServer> quoteServers = new ArrayList<QuoteServer>();
    private List<TickerServer> tickerServers = new ArrayList<TickerServer>();
    private List<Indicator> indicators = new ArrayList<Indicator>();
    private QuotePool quotePool = new QuotePool();
    private TickerPool tickerPool = new TickerPool();

    public void deleteQuotes(String symbol, Frequency freq, long fromTime, long toTime) {
    }

    public void saveQuotes(String symbol, Frequency freq, List<Quote> quotes, long sourceId) {
    }

    public List<Quote> restoreQuotes(String symbol, Frequency freq) {
        return Collections.<Quote>emptyList();
    }

    public void dropAllQuoteTables(String symbol) {
    }

    public void shutdown() {
    }

    public QuotePool getQuotePool() {
        return quotePool;
    }

    public TickerPool getTickerPool() {
        return tickerPool;
    }

    public void restoreProperties() {
    }

    public void saveProperties() {
    }

    public void saveContents(AnalysisContents contents) {
    }

    public AnalysisContents restoreContents(String symbol) {
        return new AnalysisContents(symbol);
    }

    public AnalysisContents getDefaultContents() {
        return new AnalysisContents("<Default>");
    }

    public <T extends Comparable> Collection<T> lookupAllRegisteredServices(Class<T> type, String folderName) {
        if (type == QuoteServer.class) {
            if (quoteServers.isEmpty()) {
                quoteServers.add(new YahooQuoteServer());
                quoteServers.add(new ApcQuoteServer());
            }
            return (Collection<T>) quoteServers;
        } else if (type == TickerServer.class) {
            if (tickerServers.isEmpty()) {
                tickerServers.add(new YahooTickerServer());
                tickerServers.add(new ApcTickerServer());
            }
            return (Collection<T>) tickerServers;
        } else if (type == Indicator.class) {
            if (indicators.isEmpty()) {
                indicators.add(new ARBRIndicator());
                indicators.add(new BIASIndicator());
                indicators.add(new BOLLIndicator());
                indicators.add(new CCIIndicator());
                indicators.add(new DMIIndicator());
                indicators.add(new EMAIndicator());
                indicators.add(new GMMAIndicator());
                indicators.add(new HVDIndicator());
                indicators.add(new KDIndicator());
                indicators.add(new MACDIndicator());
                indicators.add(new MAIndicator());
                indicators.add(new MFIIndicator());
                indicators.add(new MTMIndicator());
                indicators.add(new OBVIndicator());
                indicators.add(new ROCIndicator());
                indicators.add(new RSIIndicator());
                indicators.add(new SARIndicator());
                indicators.add(new VOLIndicator());
                indicators.add(new WMSIndicator());
                indicators.add(new ZIGZAGFAIndicator());
                indicators.add(new ZIGZAGIndicator());
            }
            return (Collection<T>) indicators;
        } else if (type == TickerServer.class) {
            return Collections.<T>emptyList();
        } else {
            return Collections.<T>emptyList();
        }
    }
}
