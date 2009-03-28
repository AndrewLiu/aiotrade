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
package org.aiotrade.lib.charting.descriptor;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.aiotrade.lib.math.PersistenceManager;
import org.aiotrade.lib.math.timeseries.Frequency;
import org.aiotrade.lib.math.timeseries.computable.ComputableHelper;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.math.timeseries.computable.Opt;
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisDescriptor;
import org.aiotrade.lib.math.timeseries.Ser;
import org.aiotrade.lib.util.serialization.BeansDocument;
import org.w3c.dom.Element;

/**
 *
 * @author Caoyuan Deng
 */
public class IndicatorDescriptor extends AnalysisDescriptor<Indicator> {

    private final List<Opt> opts = new ArrayList<Opt>();

    public IndicatorDescriptor() {
    }

    public IndicatorDescriptor(String serviceClassName, Frequency freq, List<Opt> opts, boolean active) {
        setServiceClassName(serviceClassName);
        setFreq(freq);
        setOpts(opts);
        setActive(active);
    }

    @Override
    public void set(String serviceClassName, Frequency freq) {
        super.set(serviceClassName, freq);

        setOptsToDefault();
    }

    public List<Opt> getOpts() {
        return opts;
    }

    public void setOpts(final List<Opt> opts) {
        /**
         * @NOTICE:
         * always create a new copy of inOpts to seperate the opts of this
         * and that transfered in (we don't know who transfer it in, so, be more
         * carefule is always good)
         */
        final int mySize = this.opts.size();
        if (opts != null) {
            for (int i = 0, n = opts.size(); i < n; i++) {
                final Opt newOpt = opts.get(i).clone();
                if (i < mySize) {
                    this.opts.set(i, newOpt);
                } else {
                    this.opts.add(newOpt);
                }
            }
        } else {
            this.opts.clear();
        }
    }

    public String getDisplayName() {
        final Indicator indicator = isServiceInstanceCreated() ? getCreatedServerInstance() : lookupServiceTemplate();

        final String displayStr = indicator == null ? getServiceClassName() : indicator.getShortDescription();

        return ComputableHelper.getDisplayName(displayStr, opts);
    }

    /**
     * @NOTICE
     * Here we get a new indicator instance by searching DefaultFileSystem(on NetBeans).
     * This is because that this instance may from other modules (i.e. SolarisIndicator),
     * it may not be seen from this module. Actually we should not set dependency on
     * those added-on modules.
     * @param baseSer for indicator
     */
    protected Indicator createServiceInstance(Object... args) {
        final Ser baseSer = (Ser) args[0];

        final Indicator template = lookupServiceTemplate();
        final Indicator instance = template == null ? null : template.createNewInstance(baseSer);
        if (instance != null) {
            if (getOpts().size() == 0) {
                /** this means this indicatorDescritor's opts may not be set yet, so set a default one now */
                setOpts(instance.getOpts());
            } else {
                /** should set opts here, because it's from those stored in xml */
                instance.setOpts(getOpts());
            }
        }

        return instance;
    }

    public void setOptsToDefault() {
        List<Opt> defaultOpts = null;
        final IndicatorDescriptor defaultDescriptor = PersistenceManager.getDefault().getDefaultContents().lookupDescriptor(
                IndicatorDescriptor.class, getServiceClassName(), getFreq());
        if (defaultDescriptor != null) {
            defaultOpts = defaultDescriptor.getOpts();
        } else {
            final Indicator template = lookupServiceTemplate();
            defaultOpts = template != null ? template.getOpts() : null;
        }

        if (defaultOpts != null) {
            setOpts(defaultOpts);
        }
    }

    public Indicator lookupServiceTemplate() {
        for (Indicator indicator : PersistenceManager.getDefault().lookupAllRegisteredServices(
                Indicator.class, getFolderName())) {
            if (indicator.getClass().getName().equals(getServiceClassName())) {
                return indicator;
            }
        }
        return null;
    }

    public static String getFolderName() {
        return "Indicators";
    }

    @Override
    public Action[] createDefaultActions() {
        return IndicatorDescriptorActionFactory.getDefault().createActions(this);
    }

    public Element writeToBean(BeansDocument doc) {
        final Element bean = super.writeToBean(doc);

        Element list = doc.listPropertyOfBean(bean, "opts");
        for (Opt opt : getOpts()) {
            doc.innerElementOfList(list, opt.writeToBean(doc));
        }

        return bean;
    }
}

