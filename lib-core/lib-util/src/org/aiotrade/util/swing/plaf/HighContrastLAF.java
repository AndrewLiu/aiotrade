/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.util.swing.plaf;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.plaf.metal.MetalLookAndFeel;



public class HighContrastLAF extends MetalLookAndFeel {

    static Properties properties = new Properties();
    // load the properties from the resource file

    static {
        try {
            properties.load(HighContrastLAF.class.getResourceAsStream("WillaHighContracstLAF.properties"));
        } catch (IOException ioe) {
            System.err.println("Could not load HighContrastLAF.properties.");
        }
        MetalLookAndFeel.setCurrentTheme(new HighContrastTheme());
    }

    @Override
    public String getName() {
        return properties.getProperty("name");
    }

    @Override
    public String getDescription() {
        return properties.getProperty("description");
    }

    @Override
    public String getID() {
        return getClass().getName();
    }

    @Override
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        double iconMagnification = 1.0;
        try {
            iconMagnification = Double.parseDouble(properties.getProperty("iconMagnificationFactor"));
        } catch (Exception exc) {
        }

        Object[] defaults = new Object[]{
            "ComboBox.selectionForeground", getHighlightedTextColor(),
            "Panel.font", getControlTextFont(),
            "CheckBox.icon", new MagnifiedIcon(MetalIconFactory.getCheckBoxIcon(), iconMagnification),
            "RadioButton.icon", new MagnifiedIcon(MetalIconFactory.getRadioButtonIcon(), iconMagnification),
            "Menu.checkIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemCheckIcon(), iconMagnification),
            "Menu.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuArrowIcon(), iconMagnification),
            "CheckBoxMenuItem.checkIcon", new MagnifiedIcon(MetalIconFactory.getCheckBoxMenuItemIcon(), iconMagnification),
            "CheckBoxMenuItem.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemArrowIcon(), iconMagnification),
            "RadioButtonMenuItem.checkIcon", new MagnifiedIcon(MetalIconFactory.getRadioButtonMenuItemIcon(), iconMagnification),
            "RadioButtonMenuItem.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemArrowIcon(), iconMagnification),
            "Tree.openIcon", new MagnifiedIcon(MetalIconFactory.getTreeFolderIcon(), iconMagnification),
            "Tree.closedIcon", new MagnifiedIcon(MetalIconFactory.getTreeFolderIcon(), iconMagnification),
            "Tree.leafIcon", new MagnifiedIcon(MetalIconFactory.getTreeLeafIcon(), iconMagnification),
            "Tree.expandedIcon", new MagnifiedIcon(MetalIconFactory.getTreeControlIcon(MetalIconFactory.DARK), iconMagnification),
            "Tree.collapsedIcon", new MagnifiedIcon(MetalIconFactory.getTreeControlIcon(MetalIconFactory.LIGHT), iconMagnification),
            "FileChooser.detailsViewIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserDetailViewIcon(), iconMagnification),
            "FileChooser.homeFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserHomeFolderIcon(), iconMagnification),
            "FileChooser.listViewIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserListViewIcon(), iconMagnification),
            "FileChooser.newFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserNewFolderIcon(), iconMagnification),
            "FileChooser.upFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserUpFolderIcon(), iconMagnification),};
        table.putDefaults(defaults);
    }

    /** A color theme which loads the main colors and fonts from an external properties file */
    protected static class HighContrastTheme extends DefaultMetalTheme {

        private FontUIResource font;

        private static ColorUIResource getColor(String key) {
            return new ColorUIResource(Integer.parseInt(properties.getProperty(key), 16));
        }

        public HighContrastTheme() {
            String fontName = properties.getProperty("fontName", "Dialog");
            int fontSize = 12;
            try {
                fontSize = Integer.parseInt(properties.getProperty("fontSize"));
            } catch (Exception exc) {
            }
            font = new FontUIResource(fontName, Font.PLAIN, fontSize);
        }

        @Override
        protected ColorUIResource getWhite() {
            return getColor("backgroundColor");
        }

        @Override
        protected ColorUIResource getBlack() {
            return getColor("foregroundColor");
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return getColor("primaryColor1");
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return getColor("primaryColor2");
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return getColor("primaryColor3");
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return getColor("secondaryColor1");
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return getColor("secondaryColor2");
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return getColor("secondaryColor3");
        }

        protected ColorUIResource getSelectionForeground() {
            return getColor("selectionForeground");
        }

        protected ColorUIResource getSelectionBackground() {
            return getColor("selectionBackground");
        }

        @Override
        public ColorUIResource getMenuSelectedBackground() {
            return getSelectionBackground();
        }

        @Override
        public ColorUIResource getMenuSelectedForeground() {
            return getSelectionForeground();
        }

        @Override
        public ColorUIResource getTextHighlightColor() {
            return getSelectionBackground();
        }

        @Override
        public ColorUIResource getHighlightedTextColor() {
            return getSelectionForeground();
        }

        @Override
        public FontUIResource getControlTextFont() {
            return font;
        }

        @Override
        public FontUIResource getMenuTextFont() {
            return font;
        }

        @Override
        public FontUIResource getSubTextFont() {
            return font;
        }

        @Override
        public FontUIResource getSystemTextFont() {
            return font;
        }

        @Override
        public FontUIResource getUserTextFont() {
            return font;
        }

        @Override
        public FontUIResource getWindowTitleFont() {
            return font;
        }
    }

    /** A class to create a magnified version of an existing icon */
    protected class MagnifiedIcon implements Icon {

        private Icon icon;
        private double factor;

        public MagnifiedIcon(Icon icon, double factor) {
            this.icon = icon;
            this.factor = factor;
        }

        public int getIconWidth() {
            return (int) (icon.getIconWidth() * factor);
        }

        public int getIconHeight() {
            return (int) (icon.getIconHeight() * factor);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.translate(x, y);
            g2d.scale(factor, factor);
            icon.paintIcon(c, g2d, 0, 0);
            g2d.dispose();
        }
    }
}
