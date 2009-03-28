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
package org.aiotrade.util.serialization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Caoyuan Deng
 */
public class BeansDocument {
    private final Document doc;
    private final Element beans;
    
    public BeansDocument() {
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        doc = builder == null ? null : builder.newDocument();
        if (doc != null) {
            beans = doc.createElement("beans");
            doc.appendChild(beans);
        } else {
            beans = null;
        }
    }
    
    public Element createBean(Object o) {
        final Element bean = doc.createElement("bean");
        bean.setAttribute("id", "" + o.hashCode());
        bean.setAttribute("class", o.getClass().getName());
        return bean;
        
    }
    
    public void appendBean(Element bean) {
        beans.appendChild(bean);
    }
    
    public Element valueConstructorArgOfBean(Element bean, int index, Object value) {
        final Element arg = doc.createElement("constructor-arg");
        arg.setAttribute("index", "" + index);
        arg.setAttribute("value", "" + value);
        bean.appendChild(arg);
        return arg;
    }
    
    public Element innerPropertyOfBean(Element bean, String name, Element innerBean) {
        final Element prop = doc.createElement("property");
        prop.setAttribute("name", name);
        prop.appendChild(innerBean);
        bean.appendChild(prop);
        return prop;
    }
    
    public Element valuePropertyOfBean(Element bean, String name, Object value) {
        final Element prop = doc.createElement("property");
        prop.setAttribute("name", name);
        prop.setAttribute("value", "" + value);
        bean.appendChild(prop);
        return prop;
    }
    
    public Element referPropertyOfBean(Element bean, String name, Object o) {
        final Element prop = doc.createElement("property");
        prop.setAttribute("name", name);
        prop.setAttribute("ref", "" + o.hashCode());
        bean.appendChild(prop);
        return prop;
    }
    
    public Element listPropertyOfBean(Element bean, String name) {
        final Element prop = doc.createElement("property");
        prop.setAttribute("name", name);
        Element list = getDoc().createElement("list");
        prop.appendChild(list);
        bean.appendChild(prop);
        return list;
    }
    
    public Element innerElementOfList(Element list, Element innerbean) {
        list.appendChild(innerbean);
        return innerbean;
    }
    
    public Element valueElementOfList(Element list, Object value) {
        final Element elem = doc.createElement("value");
        elem.setNodeValue("" + value);
        list.appendChild(elem);
        return elem;
    }
    
    public Document getDoc() {
        return doc;
    }
    
    public void saveDoc() {
        File file = new File("test.xml");
        try {
            saveToFile(new FileOutputStream(file));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
    
    public void saveToFile(FileOutputStream out) {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute("indent-number", 4);
        try {
            Transformer t = factory.newTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            /**
             * must wrap the outputstream with a writer (or bufferedwriter to
             * workaround a "buggy" behavior of the xml handling code to indent.
             */
            t.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
        } catch (TransformerFactoryConfigurationError ex) {
            ex.printStackTrace();
        } catch (TransformerException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

}
