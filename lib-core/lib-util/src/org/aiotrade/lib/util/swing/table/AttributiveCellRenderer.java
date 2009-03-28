/*
 * (swing1.1beta3)
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 * @version 1.0 11/22/98
 */
public class AttributiveCellRenderer extends DefaultTableCellRenderer {

    public AttributiveCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Color foreground = null;
        Color background = null;
        Font font = null;
        TableModel model = table.getModel();
        if (model instanceof AttributiveCellTableModel) {
            CellAttribute cellAttr = ((AttributiveCellTableModel) model).getCellAttribute();
            if (cellAttr instanceof ColoredCell) {
                foreground = ((ColoredCell) cellAttr).getForeground(row, column);
                background = ((ColoredCell) cellAttr).getBackground(row, column);
                setHorizontalAlignment(((ColoredCell) cellAttr).getHorizontalAlignment(row, column));
            }
            if (cellAttr instanceof CellFont) {
                font = ((CellFont) cellAttr).getFont(row, column);
            }
        }

        if (isSelected) {
            setForeground(foreground != null ? foreground : table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(foreground != null ? foreground : table.getForeground());
            setBackground(background != null ? background : table.getBackground());
        }

        setFont(font != null ? font : table.getFont());

        if (hasFocus) {
            Border border = null;
            if (isSelected) {
                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
            }
            setBorder(border);

            if (!isSelected && table.isCellEditable(row, column)) {
                setForeground((foreground != null) ? foreground : UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(noFocusBorder);
        }

        setValue(value);

        return this;
    }

    @Override
    protected void setValue(Object value) {
        setText(value == null ? "" : value.toString());
    }
}


