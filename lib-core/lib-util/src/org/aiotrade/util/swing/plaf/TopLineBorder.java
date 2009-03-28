package org.aiotrade.util.swing.plaf;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * A class which implements a top line border of arbitrary thickness
 * and of a single color.
 *
 * @author Caoyuan Deng
 */
public class TopLineBorder extends AbstractBorder {

    private static Border blackLine;
    private static Border grayLine;
    protected int thickness;
    protected Color lineColor;
    protected boolean roundedCorners;

    /**
     * Convenience method for getting the Color.black LineBorder of thickness 1.
     */
    public static Border createBlackLineBorder() {
        if (blackLine == null) {
            blackLine = new LineBorder(Color.black, 1);
        }
        return blackLine;
    }

    /**
     * Convenience method for getting the Color.gray LineBorder of thickness 1.
     */
    public static Border createGrayLineBorder() {
        if (grayLine == null) {
            grayLine = new LineBorder(Color.gray, 1);
        }
        return grayLine;
    }

    /**
     * Creates a line border with the specified color and a thickness = 1.
     * @param color the color for the border
     */
    public TopLineBorder(Color color) {
        this(color, 1, false);
    }

    /**
     * Creates a line border with the specified color and thickness.
     * @param color the color of the border
     * @param thickness the thickness of the border
     */
    public TopLineBorder(Color color, int thickness) {
        this(color, thickness, false);
    }

    /**
     * Creates a line border with the specified color, thickness, and corner shape.
     * @param color the color of the border
     * @param thickness the thickness of the border
     * @param roundedCorners whether or not border corners should be round
     */
    public TopLineBorder(Color color, int thickness, boolean roundedCorners) {
        lineColor = color;
        this.thickness = thickness;
        this.roundedCorners = roundedCorners;
    }

    /**
     * Paints the border for the specified component with the
     * specified position and size.
     * @param c the component for which this border is being painted
     * @param g the paint graphics
     * @param x the x position of the painted border
     * @param y the y position of the painted border
     * @param width the width of the painted border
     * @param height the height of the painted border
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        int i;

        g.setColor(lineColor);
        for (i = 0; i < thickness; i++) {
            if (!roundedCorners) {
                g.drawLine(x + i, y + i, width - i - i - 1, y + i);
            } else {
                //g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, thickness, thickness);
            }
        }
        g.setColor(oldColor);
    }

    /**
     * Returns the insets of the border.
     * @param c the component for which this border insets value applies
     */
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = thickness;
        return insets;
    }

    /**
     * Returns the color of the border.
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * Returns the thickness of the border.
     */
    public int getThickness() {
        return thickness;
    }

    /**
     * Returns whether this border will be drawn with rounded corners.
     */
    public boolean getRoundedCorners() {
        return roundedCorners;
    }

    /**
     * Returns whether or not the border is opaque.
     */
    @Override
    public boolean isBorderOpaque() {
        return !roundedCorners;
    }
}
