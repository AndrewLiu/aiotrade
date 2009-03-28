/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.util;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 *
 * @author dcaoyuan
 */
public class Bundle {
    private static String RESOURCE_NAME = "Bundle";
    private static Map<Class, ResourceBundle> cache = new HashMap<Class, ResourceBundle>();

    public static String getString(Class clazz, String name) {
        return getResourceBundle(clazz).getString(name);
    }

    private static ResourceBundle getResourceBundle(Class clazz) {
        ResourceBundle rb = cache.get(clazz);
        if (rb == null) {
            String name = clazz.getName();
            int dotIdx = name.lastIndexOf('.');
            if (dotIdx != -1) {
                name = name.substring(0, dotIdx) + "." + RESOURCE_NAME;
            } else {
                name = RESOURCE_NAME;
            }
            rb = ResourceBundle.getBundle(name);
            cache.put(clazz, rb);
        }
        return rb;
    }
}
