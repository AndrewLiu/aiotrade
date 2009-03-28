/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;

/**
 * @version 1.0 11/22/98
 */
public class DefaultCellAttribute implements CellAttribute, CellSpan, ColoredCell, CellFont {

    //
    // !!!! CAUTION !!!!!
    // these values must be synchronized to Table data
    //
    protected int rowSize;
    protected int columnSize;
    protected int[][][] spans;                   // CellSpan
    private Attr[][] attrs;

    private enum AttrType {

        Foreground,
        Background,
        Font,
        HorizontalAlignment,
        VerticalAlignment
    }

    private final class Attr {

        Color foreground;
        Color background;
        Font font;
        int horizontalAlignment;
        int verticalAlignment;
    }

    public DefaultCellAttribute() {
        this(1, 1);
    }

    public DefaultCellAttribute(int numRows, int numColumns) {
        setSize(new Dimension(numColumns, numRows));
    }

    protected void initValue() {
        for (int i = 0; i < spans.length; i++) {
            for (int j = 0; j < spans[i].length; j++) {
                spans[i][j][CellSpan.COLUMN] = 1;
                spans[i][j][CellSpan.ROW] = 1;
            }
        }
    }

    //
    // CellSpan
    //
    public int[] getSpan(int row, int column) {
        if (isOutOfBounds(row, column)) {
            int[] ret_code = {1, 1};
            return ret_code;
        }
        return spans[row][column];
    }

    public void setSpan(int[] span, int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        this.spans[row][column] = span;
    }

    public boolean isVisible(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return false;
        }
        if ((spans[row][column][CellSpan.COLUMN] < 1) || (spans[row][column][CellSpan.ROW] < 1)) {
            return false;
        }
        return true;
    }

    public void combine(int[] rows, int[] columns) {
        if (isOutOfBounds(rows, columns)) {
            return;
        }
        int rowSpan = rows.length;
        int columnSpan = columns.length;
        int startRow = rows[0];
        int startColumn = columns[0];
        for (int i = 0; i < rowSpan; i++) {
            for (int j = 0; j < columnSpan; j++) {
                if ((spans[startRow + i][startColumn + j][CellSpan.COLUMN] != 1) || (spans[startRow + i][startColumn + j][CellSpan.ROW] != 1)) {
                    //System.out.println("can't combine");
                    return;
                }
            }
        }
        for (int i = 0, ii = 0; i < rowSpan; i++, ii--) {
            for (int j = 0, jj = 0; j < columnSpan; j++, jj--) {
                spans[startRow + i][startColumn + j][CellSpan.COLUMN] = jj;
                spans[startRow + i][startColumn + j][CellSpan.ROW] = ii;
                //System.out.println("r " +ii +"  c " +jj);
            }
        }
        spans[startRow][startColumn][CellSpan.COLUMN] = columnSpan;
        spans[startRow][startColumn][CellSpan.ROW] = rowSpan;

    }

    public void split(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        int columnSpan = spans[row][column][CellSpan.COLUMN];
        int rowSpan = spans[row][column][CellSpan.ROW];
        for (int i = 0; i < rowSpan; i++) {
            for (int j = 0; j < columnSpan; j++) {
                spans[row + i][column + j][CellSpan.COLUMN] = 1;
                spans[row + i][column + j][CellSpan.ROW] = 1;
            }
        }
    }

    //
    // ColoredCell
    //
    public Color getForeground(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return null;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            return null;
        } else {
            return attr.foreground;
        }
    }

    public void setForeground(Color color, int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            attr = new Attr();
            attrs[row][column] = attr;
        }
        attr.foreground = color;
    }

    public void setForeground(Color color, int[] rows, int[] columns) {
        if (isOutOfBounds(rows, columns)) {
            return;
        }
        setAttributes(AttrType.Foreground, color, rows, columns);
    }

    public Color getBackground(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return null;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            return null;
        } else {
            return attr.background;
        }
    }

    public void setBackground(Color color, int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            attr = new Attr();
            attrs[row][column] = attr;
        }
        attr.background = color;
    }

    public void setBackground(Color color, int[] rows, int[] columns) {
        if (isOutOfBounds(rows, columns)) {
            return;
        }
        setAttributes(AttrType.Background, color, rows, columns);
    }
    //

    //
    // CellFont
    //
    public Font getFont(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return null;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            return null;
        } else {
            return attr.font;
        }
    }

    public void setFont(Font font, int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            attr = new Attr();
            attrs[row][column] = attr;
        }
        attr.font = font;
    }

    public void setFont(Font font, int[] rows, int[] columns) {
        if (isOutOfBounds(rows, columns)) {
            return;
        }
        setAttributes(AttrType.Font, font, rows, columns);
    }

    public int getHorizontalAlignment(int row, int column) {
        if (isOutOfBounds(row, column)) {
            return JLabel.LEADING;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            return JLabel.LEADING;
        } else {
            return attr.horizontalAlignment;
        }
    }

    public void setHorizontalAlignment(int horizontalAlignment, int row, int column) {
        if (isOutOfBounds(row, column)) {
            return;
        }
        Attr attr = attrs[row][column];
        if (attr == null) {
            attr = new Attr();
            attrs[row][column] = attr;
        }
        attr.horizontalAlignment = horizontalAlignment;
    }
    //

    //
    // CellAttribute
    //
    public void addColumn() {
        int[][][] oldSpan = spans;
        int numRows = oldSpan.length;
        int numColumns = oldSpan[0].length;
        spans = new int[numRows][numColumns + 1][2];
        System.arraycopy(oldSpan, 0, spans, 0, numRows);
        for (int i = 0; i < numRows; i++) {
            spans[i][numColumns][CellSpan.COLUMN] = 1;
            spans[i][numColumns][CellSpan.ROW] = 1;
        }

        Attr[][] oldAttr = attrs;
        attrs = new Attr[numRows][numColumns + 1];
        System.arraycopy(oldAttr, 0, attrs, 0, numRows);
    }

    public void addRow() {
        int[][][] oldSpan = spans;
        int numRows = oldSpan.length;
        int numColumns = oldSpan[0].length;

        spans = new int[numRows + 1][numColumns][2];
        System.arraycopy(oldSpan, 0, spans, 0, numRows);
        for (int i = 0; i < numColumns; i++) {
            spans[numRows][i][CellSpan.COLUMN] = 1;
            spans[numRows][i][CellSpan.ROW] = 1;
        }

        Attr[][] oldAttr = attrs;
        attrs = new Attr[numRows][numColumns + 1];
        System.arraycopy(oldAttr, 0, attrs, 0, numRows);
    }

    public void insertRow(int row) {
        int[][][] oldSpan = spans;
        int numRows = oldSpan.length;
        int numColumns = oldSpan[0].length;
        spans = new int[numRows + 1][numColumns][2];
        if (0 < row) {
            System.arraycopy(oldSpan, 0, spans, 0, row - 1);
        }
        System.arraycopy(oldSpan, 0, spans, row, numRows - row);
        for (int i = 0; i < numColumns; i++) {
            spans[row][i][CellSpan.COLUMN] = 1;
            spans[row][i][CellSpan.ROW] = 1;
        }
    }

    public Dimension getSize() {
        return new Dimension(rowSize, columnSize);
    }

    public void setSize(Dimension size) {
        columnSize = size.width;
        rowSize = size.height;
        spans = new int[rowSize][columnSize][2];   // 2: COLUMN,ROW
        attrs = new Attr[rowSize][columnSize];
        initValue();
    }

    /*
    public void changeAttribute(int row, int column, Object command) {
    }

    public void changeAttribute(int[] rows, int[] columns, Object command) {
    }
     */
    protected boolean isOutOfBounds(int row, int column) {
        if ((row < 0) || (rowSize <= row) || (column < 0) || (columnSize <= column)) {
            return true;
        }
        return false;
    }

    protected boolean isOutOfBounds(int[] rows, int[] columns) {
        for (int i = 0; i < rows.length; i++) {
            if ((rows[i] < 0) || (rowSize <= rows[i])) {
                return true;
            }
        }
        for (int i = 0; i < columns.length; i++) {
            if ((columns[i] < 0) || (columnSize <= columns[i])) {
                return true;
            }
        }
        return false;
    }

    private void setAttributes(AttrType type, Object value, int[] rows, int[] columns) {
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            for (int j = 0; j < columns.length; j++) {
                int column = columns[j];
                Attr attr = attrs[row][column];
                if (attr == null) {
                    attr = new Attr();
                    attrs[row][column] = attr;
                }
                switch (type) {
                    case Foreground:
                        attr.foreground = (Color) value;
                        break;
                    case Background:
                        attr.background = (Color) value;
                        break;
                    case Font:
                        attr.font = (Font) value;
                        break;
                    case HorizontalAlignment:
                        attr.horizontalAlignment = (Integer) value;
                        break;
                    case VerticalAlignment:
                        attr.verticalAlignment = (Integer) value;
                        break;
                }
            }
        }
    }
}
