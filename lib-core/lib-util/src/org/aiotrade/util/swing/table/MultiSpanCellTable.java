/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.util.swing.table;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * @version 1.0 11/26/98
 */
public class MultiSpanCellTable extends JTable {

    public MultiSpanCellTable(TableModel model) {
        super(model);
        setUI(new MultiSpanCellTableUI());
        getTableHeader().setReorderingAllowed(false);
        setCellSelectionEnabled(true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    }

    @Override
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        if (row < 0 || column < 0 || getRowCount() <= row || getColumnCount() <= column) {
            return super.getCellRect(row, column, includeSpacing);
        }

        CellSpan cellAtt = (CellSpan) ((AttributiveCellTableModel) getModel()).getCellAttribute();
        if (!cellAtt.isVisible(row, column)) {
            int temp_row = row;
            int temp_column = column;
            row += cellAtt.getSpan(temp_row, temp_column)[CellSpan.ROW];
            column += cellAtt.getSpan(temp_row, temp_column)[CellSpan.COLUMN];
        }
        int[] spans = cellAtt.getSpan(row, column);

        TableColumnModel cmodel = getColumnModel();
        int cm = cmodel.getColumnMargin();
        Rectangle r = new Rectangle();
        int aCellHeight = rowHeight + rowMargin;
        r.y = row * aCellHeight;
        r.height = spans[CellSpan.ROW] * aCellHeight;

        if (getComponentOrientation().isLeftToRight()) {
            for (int i = 0; i < column; i++) {
                r.x += cmodel.getColumn(i).getWidth();
            }
        } else {
            for (int i = cmodel.getColumnCount() - 1; i > column; i--) {
                r.x += cmodel.getColumn(i).getWidth();
            }
        }
        r.width = cmodel.getColumn(column).getWidth();
        
        for (int i = 0; i < spans[CellSpan.COLUMN] - 1; i++) {
            r.width += cmodel.getColumn(column + i).getWidth() + cm;
        }

        if (!includeSpacing) {
            int rm = getRowMargin();
            r.setBounds(r.x + cm / 2, r.y + rm / 2, r.width - cm, r.height - rm);
        }
        return r;
    }

    private int[] rowColumnAtPoint(Point point) {
        int[] retValue = {-1, -1};
        int row = point.y / (rowHeight + rowMargin);
        if ((row < 0) || (getRowCount() <= row)) {
            return retValue;
        }
        int column = getColumnModel().getColumnIndexAtX(point.x);

        CellSpan cellAtt = (CellSpan) ((AttributiveCellTableModel) getModel()).getCellAttribute();

        if (cellAtt.isVisible(row, column)) {
            retValue[CellSpan.COLUMN] = column;
            retValue[CellSpan.ROW] = row;
            return retValue;
        }
        retValue[CellSpan.COLUMN] = column + cellAtt.getSpan(row, column)[CellSpan.COLUMN];
        retValue[CellSpan.ROW] = row + cellAtt.getSpan(row, column)[CellSpan.ROW];
        return retValue;
    }

    @Override
    public int rowAtPoint(Point point) {
        return rowColumnAtPoint(point)[CellSpan.ROW];
    }

    @Override
    public int columnAtPoint(Point point) {
        return rowColumnAtPoint(point)[CellSpan.COLUMN];
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        repaint();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int firstIndex = e.getFirstIndex();
        int lastIndex = e.getLastIndex();
        if (firstIndex == -1 && lastIndex == -1) { // Selection cleared.
            repaint();
        }
        Rectangle dirtyRegion = getCellRect(firstIndex, 0, false);
        int numCoumns = getColumnCount();
        int index = firstIndex;
        for (int i = 0; i < numCoumns; i++) {
            dirtyRegion.add(getCellRect(index, i, false));
        }
        index = lastIndex;
        for (int i = 0; i < numCoumns; i++) {
            dirtyRegion.add(getCellRect(index, i, false));
        }
        repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
    }
}

