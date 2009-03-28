/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.util.swing.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * This border style is very good for Applet display
 *
 * @author dcaoyuan
 */
public class AIOScrollPaneStyleBorder extends AbstractBorder {

    private Color color;
    private boolean needShadow;

    public AIOScrollPaneStyleBorder() {
        super();
    }

    public AIOScrollPaneStyleBorder(Color color) {
        this(color, false);
    }

    public AIOScrollPaneStyleBorder(Color color, boolean needShadow) {
        this.color = color;
        this.needShadow = needShadow;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {

        g.translate(x, y);

        if (color != null) {
            g.setColor(color);
        } else {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
        }
        g.drawRect(0, 0, w - 1, h - 1);

        if (needShadow) {
            // paint shadow
            if (color != null) {
                g.setColor(color);
            } else {
                g.setColor(MetalLookAndFeel.getControlHighlight());
            }
            g.drawLine(w - 1, 1, w - 1, h - 1);
            g.drawLine(1, h - 1, w - 1, h - 1);

            if (color != null) {
                g.setColor(color);
            } else {
                g.setColor(MetalLookAndFeel.getControl());
            }
            g.drawLine(w - 2, 2, w - 2, 2);
            g.drawLine(1, h - 2, 1, h - 2);

            g.translate(-x, -y);
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(1, 1, 2, 2);
        return insets;
    }
}
