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
package org.aiotrade.lib.charting.view.pane;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.aiotrade.lib.charting.laf.LookFeel;

/**
 *
 * @author Caoyuan Deng
 */
public abstract class AbstractScrollControl extends JComponent {

    private final static int SCROLL_SPEED_THROTTLE = 60; // delay in milli seconds
    private final static Cursor RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private final static Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private Composite arrowComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    private Composite thumbComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    private Composite trackComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    private DecreaseArrow decrArrow;
    private IncreaseArrow incrArrow;
    private Track track;
    private Thumb thumb;
    private boolean readyToDragThumb, readyToDragThumbBegSide, readyToDragThumbEndSide;
    private double mousePressedThumbBeg, mousePressedThumbEnd;
    private int xMousePressed, yMousePressed;
    private MyMouseAdapter myMouseAdapter;
    private Position mousePressedAt;
    private Timer scrollTimer;
    private ScrollTimerListener scrollTimerListener;
    private GeneralPath bufPath = new GeneralPath();
    private boolean scalable;
    private boolean extendable;
    private boolean autoHidden;
    private boolean hidden;

    private enum Position {

        OnDecreaseArrow,
        OnIncreaseArrow,
        OnThumb,
        OnThumbBegSide,
        OnThumbEndSide,
        AfterThumb,
        BeforeThumb,
        DontCare;
    }

    public AbstractScrollControl() {
        setOpaque(false);

        setLayout(new BorderLayout());

        decrArrow = new DecreaseArrow();
        decrArrow.setPreferredSize(new Dimension(12, 12));
        decrArrow.setEnabled(true);
        decrArrow.setVisible(true);
        add(decrArrow, BorderLayout.WEST);

        incrArrow = new IncreaseArrow();
        incrArrow.setPreferredSize(new Dimension(12, 12));
        incrArrow.setEnabled(true);
        incrArrow.setVisible(true);
        add(incrArrow, BorderLayout.EAST);

        track = new Track();
        track.setPreferredSize(new Dimension(12, 12));
        track.setEnabled(true);
        track.setVisible(true);
        add(track, BorderLayout.CENTER);

        thumb = new Thumb();

        scrollTimerListener = new ScrollTimerListener();
        scrollTimer = new Timer(SCROLL_SPEED_THROTTLE, scrollTimerListener);
        scrollTimer.setInitialDelay(300);  // default InitialDelay

        myMouseAdapter = new MyMouseAdapter();
        addMouseListener(myMouseAdapter);
        addMouseMotionListener(myMouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (isHidden()) {
            return;
        }

        Graphics2D g = (Graphics2D) g0;
        setForeground(LookFeel.getCurrent().axisColor);
        g.setColor(getForeground());

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        bufPath.reset();
        /** plot upper border lines */
        bufPath.moveTo(0, 0);
        bufPath.lineTo(w, 0);

        g.draw(bufPath);
    }

    public void setAutoHidden(boolean b) {
        if (b != this.autoHidden) {
            this.autoHidden = b;

            repaint();
        }
    }

    public boolean isAutoHidden() {
        return autoHidden;
    }

    private void setHidden(boolean b) {
        if (b != this.hidden) {
            this.hidden = b;

            if (autoHidden) {
                repaint();
            }
        }
    }

    private boolean isHidden() {
        return autoHidden && hidden;
    }

    public void setValues(double valueModelRange, double valueShownRange, double valueModelEnd, double valueShownEnd, double unit, int nUnitsBlock) {
        /**
         * @NOTICE
         * all parameters is defined in Thumb, so we need to re-set thumb's geom
         * by values that are transfered in
         */
        thumb.setGeometryBy(valueModelRange, valueShownRange, valueModelEnd, valueShownEnd, unit, nUnitsBlock);
    }

    public double getUnit() {
        return thumb.getUnit();
    }

    public int getNUnitsBlock() {
        return thumb.getNUnitsBlock();
    }

    public double getValueShownBeg() {
        return thumb.getValueShownBeg();
    }

    public double getValueShownEnd() {
        return thumb.getValueShownEnd();
    }

    public double getValueShownRange() {
        return thumb.getValueShownRange();
    }

    public void setScalable(boolean b) {
        this.scalable = b;
    }

    public void setExtendable(boolean b) {
        this.extendable = b;
    }

    public boolean isScalable() {
        return scalable;
    }

    public boolean isExtendable() {
        return extendable;
    }

    /**
     * @NOTICE
     * Whether the mouse is acted on arrows or thumb, always do scrolling or
     * scaling via thumb
     */
    private class MyMouseAdapter extends MouseAdapter implements MouseMotionListener {

        @Override
        public void mousePressed(MouseEvent e) {
            if (isHidden()) {
                return;
            }

            xMousePressed = e.getX();
            yMousePressed = e.getY();

            Rectangle thumbRect = thumb.getBounds();
            mousePressedThumbBeg = thumbRect.x;
            mousePressedThumbEnd = thumbRect.x + thumbRect.width;

            /** set all the readyToDrag to false first, will be decided late */
            readyToDragThumb = false;
            readyToDragThumbBegSide = false;
            readyToDragThumbEndSide = false;


            switch (getMousePressedPosition()) {
                case OnDecreaseArrow:
                    thumb.scrollByUnit(-1);

                    scrollTimer.stop();
                    scrollTimerListener.setNumberWithDirection(-1);
                    scrollTimerListener.setUseBlockIncrement(false);
                    scrollTimerListener.startScrollTimerIfNecessary();

                    break;
                case OnIncreaseArrow:
                    thumb.scrollByUnit(+1);

                    scrollTimer.stop();
                    scrollTimerListener.setNumberWithDirection(+1);
                    scrollTimerListener.setUseBlockIncrement(false);
                    scrollTimerListener.startScrollTimerIfNecessary();

                    break;
                case OnThumb:
                    readyToDragThumb = true;

                    break;
                case OnThumbBegSide:
                    if (!isScalable()) {
                        break;
                    }

                    readyToDragThumbBegSide = true;

                    break;
                case OnThumbEndSide:
                    if (!isScalable()) {
                        break;
                    }

                    readyToDragThumbEndSide = true;


                    break;
                case BeforeThumb:
                    thumb.scrollByBlock(-1);

                    scrollTimer.stop();
                    scrollTimerListener.setNumberWithDirection(-1);
                    scrollTimerListener.setUseBlockIncrement(true);
                    scrollTimerListener.startScrollTimerIfNecessary();

                    break;
                case AfterThumb:
                    thumb.scrollByBlock(+1);

                    scrollTimer.stop();
                    scrollTimerListener.setNumberWithDirection(+1);
                    scrollTimerListener.setUseBlockIncrement(true);
                    scrollTimerListener.startScrollTimerIfNecessary();

                    break;
                default:
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            scrollTimer.stop();

            readyToDragThumb = false;
            readyToDragThumbBegSide = false;
            readyToDragThumbEndSide = false;
        }

        public void mouseDragged(MouseEvent e) {
            if (isHidden()) {
                return;
            }

            if (readyToDragThumb) {

                double xMoved = e.getX() - xMousePressed;

                double newThumbBeg = mousePressedThumbBeg + xMoved;
                thumb.drag(newThumbBeg);

            } else if (readyToDragThumbBegSide) {

                double xMoved = e.getX() - xMousePressed;

                double newThumbBeg = mousePressedThumbBeg + xMoved;
                thumb.dragBegSide(newThumbBeg);

            } else if (readyToDragThumbEndSide) {

                double xMoved = e.getX() - xMousePressed;

                double newThumbEnd = mousePressedThumbEnd + xMoved;
                thumb.dragEndSide(newThumbEnd);
            }

            /** don't forget to update to the new mouse pressed position */
            xMousePressed = e.getX();
            yMousePressed = e.getY();

            Rectangle thumbRect = thumb.getBounds();
            mousePressedThumbBeg = thumbRect.x;
            mousePressedThumbEnd = thumbRect.x + thumbRect.width;
        }

        public void mouseMoved(MouseEvent e) {
            if (isHidden()) {
                return;
            }

            if (!isScalable()) {
                return;
            }

            setCursor(DEFAULT_CURSOR);

            double xMouse = e.getX();
            double yMouse = e.getY();

            Rectangle thumbRect = thumb.getBounds();
            double xThumbRectBeg = thumbRect.x;
            double xThumbRectEnd = thumbRect.x + thumbRect.width;

            if (xMouse > xThumbRectBeg - 1 && xMouse < xThumbRectBeg + 1) {
                /** on thumbBegSide */
                setCursor(RESIZE_CURSOR);
            } else if (xMouse > xThumbRectEnd - 1 && xMouse < xThumbRectEnd + 1) {
                /** on thumbEndSide */
                setCursor(RESIZE_CURSOR);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setHidden(false);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setHidden(true);
        }
    }

    private Position getMousePressedPosition() {
        if (yMousePressed < 0 || yMousePressed > getHeight()) {
            return Position.DontCare;
        }

        Rectangle decrArrowRect = decrArrow.getBounds();
        if (xMousePressed > decrArrowRect.x && xMousePressed < decrArrowRect.x + decrArrowRect.width) {
            return Position.OnDecreaseArrow;
        }

        Rectangle incrArrowRect = incrArrow.getBounds();
        if (xMousePressed > incrArrowRect.x && xMousePressed < incrArrowRect.x + incrArrowRect.width) {
            return Position.OnIncreaseArrow;
        }

        Rectangle thumbRect = thumb.getBounds();
        double xThumbRectBeg = thumbRect.x;
        double xThumbRectEnd = thumbRect.x + thumbRect.width;
        if (xMousePressed >= xThumbRectBeg + 1 && xMousePressed <= xThumbRectEnd - 1) {
            return Position.OnThumb;
        }

        if (xMousePressed > xThumbRectBeg - 1 && xMousePressed < xThumbRectBeg + 1) {
            return Position.OnThumbBegSide;
        }

        if (xMousePressed > xThumbRectEnd - 1 && xMousePressed < xThumbRectEnd + 1) {
            return Position.OnThumbEndSide;
        }

        if (xMousePressed < xThumbRectBeg && xMousePressed > decrArrowRect.x + decrArrowRect.width) {
            return Position.BeforeThumb;
        }

        if (xMousePressed > xThumbRectEnd && xMousePressed < incrArrowRect.x) {
            return Position.AfterThumb;
        }

        return Position.DontCare;
    }

    private class DecreaseArrow extends JComponent {

        private GeneralPath bufPath = new GeneralPath();

        public DecreaseArrow() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (isHidden()) {
                return;
            }

            Graphics2D g = (Graphics2D) g0;
            Composite backupComposite = g.getComposite();
            g.setComposite(arrowComposite);

            setForeground(LookFeel.getCurrent().axisColor);
            g.setColor(getForeground());

            int w = getWidth();
            int h = getHeight();
            int wc = w / 2;
            int hc = h / 2;

            /** draw border */
            g.drawRect(0, 0, w - 1, h - 1);

            /** draw left arrow */
            bufPath.reset();
            bufPath.moveTo(wc - 4, hc);
            bufPath.lineTo(wc + 3, hc - 4);
            bufPath.lineTo(wc + 3, hc + 4);
            bufPath.closePath();
            g.fill(bufPath);

            if (backupComposite != null) {
                g.setComposite(backupComposite);
            }
        }
    }

    private class IncreaseArrow extends JComponent {

        private GeneralPath bufPath = new GeneralPath();

        public IncreaseArrow() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (isHidden()) {
                return;
            }

            Graphics2D g = (Graphics2D) g0;
            Composite backupComposite = g.getComposite();
            g.setComposite(arrowComposite);

            setForeground(LookFeel.getCurrent().axisColor);
            g.setColor(getForeground());

            int w = getWidth();
            int h = getHeight();
            int wc = w / 2;
            int hc = h / 2;

            /** draw border */
            g.drawRect(0, 0, w - 1, h - 1);

            /** draw right arrow */
            bufPath.reset();
            bufPath.moveTo(wc + 4, hc);
            bufPath.lineTo(wc - 3, hc - 4);
            bufPath.lineTo(wc - 3, hc + 4);
            bufPath.closePath();
            g.fill(bufPath);

            if (backupComposite != null) {
                g.setComposite(backupComposite);
            }
        }
    }

    private class Track extends JComponent {

        private GeneralPath bufPath = new GeneralPath();
        private Rectangle bufRect = new Rectangle();

        public Track() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (isHidden()) {
                return;
            }

            float w = getWidth();
            float h = getHeight();

            Graphics2D g = (Graphics2D) g0;
            Composite backupComposite = g.getComposite();
            /** draw track */
            bufRect.setRect(0.0, 1.0, w, h - 1.0);

            setForeground(LookFeel.getCurrent().getTrackColor());
            g.setColor(getForeground());
            g.setComposite(trackComposite);
            g.fill(bufRect);

            if (backupComposite != null) {
                g.setComposite(backupComposite);
            }

            thumb.paintOn(g0, getBounds());
        }
    }

    /**
     * Thumb is not a JComponent, is will be painted by Track
     */
    private class Thumb {

        private double trackBeg;   // the most left absolute position's x of track
        private double trackEnd;   // the most right absolute position's x of track
        private double wOne;       // width in pixels per 1.0 (pixels / value)
        private double valueModelRange; // such as nBars of ser
        private double valueModelBeg;   // such as last position of ser items
        private double valueModelEnd;   // such as last position of ser items
        private double valueShownRange; // such as nBars of view
        private double valueShownBeg;
        private double valueShownEnd;   // such as end position of ser shown on screen(a movable window of all items)
        private double thumbRange;
        private double thumbBeg;   // absolute position in XContenetPane
        private double thumbEnd;   // absolute position in XContenetPane
        private double unit = 1;   // value of unit, in thumb and track, one unit means 1 pixel, in valueModel and valueShown, is value: 1.0f
        private int nUnitsBlock = 1;
        private Rectangle thumbBounds = new Rectangle();
        private Rectangle bufRect = new Rectangle();

        /**
         * track <-> model
         * thumb <-> shown
         */
        private void setGeometryBy(double valueModelRange, double valueShownRange, double valueModelEnd, double valueShownEnd, double unit, int blockNUnits) {
            this.valueModelRange = valueModelRange;
            this.valueShownRange = valueShownRange;
            this.valueModelEnd = valueModelEnd;
            this.valueShownEnd = valueShownEnd;
            this.unit = unit;
            this.nUnitsBlock = blockNUnits;

            this.valueModelBeg = valueModelEnd - valueModelRange;
            this.valueShownBeg = valueShownEnd - valueShownRange;

            Rectangle trackRect = track.getBounds();
            this.trackBeg = trackRect.x;
            this.trackEnd = trackRect.x + trackRect.width;

            this.wOne = (trackEnd - trackBeg) / (valueModelEnd - valueModelBeg);

            this.thumbBeg = xv(valueShownBeg);
            this.thumbEnd = xv(valueShownEnd);
            this.thumbRange = thumbEnd - thumbBeg;
        }

        private double xv(double v) {
            return trackBeg + wOne * (v - valueModelBeg);
        }

        private double vx(double x) {
            return (x - trackBeg) / wOne + valueModelBeg;
        }

        private void setThumb(double thumbBeg, double thumbEnd) {
            this.thumbBeg = thumbBeg;
            this.thumbEnd = thumbEnd;
            this.thumbRange = thumbEnd - thumbBeg;
        }

        private void setValueShown(double valueShownBeg, double valueShownEnd) {
            this.valueShownBeg = valueShownBeg;
            this.valueShownEnd = valueShownEnd;
            this.valueShownRange = valueShownEnd - valueShownBeg;
        }

        protected Rectangle getBounds() {
            thumbBounds.setRect(thumbBeg, 0, thumbRange, getHeight());

            return thumbBounds;
        }

        protected double getUnit() {
            return unit;
        }

        protected int getNUnitsBlock() {
            return nUnitsBlock;
        }

        protected double getValueShownBeg() {
            return valueShownBeg;
        }

        protected double getValueShownEnd() {
            return valueShownEnd;
        }

        protected double getValueShownRange() {
            return valueShownRange;
        }

        protected void scrollByUnit(double nUnitsWithDirection) {
            double nUnitsMoved = nUnitsWithDirection;
            double valueMoved = nUnitsMoved * unit;
            double newValueShownBeg = valueShownBeg + valueMoved;
            double newValueShownEnd = valueShownEnd + valueMoved;
            double newThumbBeg = xv(newValueShownBeg);
            double newThumbEnd = xv(newValueShownEnd);

            if (!isExtendable()) {
                if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
                    return;
                }
            }

            setValueShown(newValueShownBeg, newValueShownEnd);
            setThumb(newThumbBeg, newThumbEnd);

            viewScrolledByUnit(nUnitsMoved);
        }

        protected void scrollByBlock(double nBlocksWithDirection) {
            scrollByUnit(nBlocksWithDirection * nUnitsBlock);
        }

        protected void drag(double newThumbBeg) {
            double xMoved = newThumbBeg - thumbBeg;
            double newThumbEnd = newThumbBeg + thumbRange;
            double newValueShownBeg = vx(newThumbBeg);
            double newValueShownEnd = vx(newThumbEnd);
            double valueMoved = newValueShownBeg - valueShownBeg;
            double nUnitsMoved = valueMoved / unit;

            if (!isExtendable()) {
                if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
                    return;
                }
            }

            setThumb(newThumbBeg, newThumbEnd);
            setValueShown(newValueShownBeg, newValueShownEnd);

            viewScrolledByUnit(nUnitsMoved);
        }

        protected void dragBegSide(double newThumbBeg) {
            double xMoved = newThumbBeg - thumbBeg;
            double newThumbEnd = thumbEnd; // keeps the same what ever
            double newThumbRange = newThumbEnd - newThumbBeg;
            if (newThumbRange < 4) {
                /** thumbRange should be at lease size of: */
                newThumbRange = 4;
                xMoved = thumbRange - newThumbRange;
                newThumbBeg = thumbBeg + xMoved;
                newThumbEnd = newThumbBeg + newThumbRange;
            }
            double newValueShownBeg = vx(newThumbBeg);
            double newValueShownEnd = vx(newThumbEnd);

            if (!isExtendable()) {
                if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
                    return;
                }
            }

            setThumb(newThumbBeg, newThumbEnd);
            setValueShown(newValueShownBeg, newValueShownEnd);

            viewScaledToRange(valueShownRange);
        }

        protected void dragEndSide(double newThumbEnd) {
            double xMoved = newThumbEnd - thumbEnd;
            double newThumbBeg = thumbBeg; // keeps the same what ever
            double newThumbRange = newThumbEnd - newThumbBeg;
            if (newThumbRange < 4) {
                /** thumbRange should be at lease size of: */
                newThumbRange = 4;
                xMoved = newThumbRange - thumbRange;
                newThumbEnd = thumbEnd + xMoved;
                newThumbBeg = newThumbEnd - newThumbRange;
            }
            double newValueShownBeg = vx(newThumbBeg);
            double newValueShownEnd = vx(newThumbEnd);
            double valueMoved = newValueShownEnd - valueShownEnd;
            double nUnitsMoved = valueMoved / unit;

            if (!isExtendable()) {
                if (newThumbBeg < trackBeg || newThumbEnd > trackEnd) {
                    return;
                }
            }

            setThumb(newThumbBeg, newThumbEnd);
            setValueShown(newValueShownBeg, newValueShownEnd);

            viewScrolledByUnit(nUnitsMoved);

            viewScaledToRange(valueShownRange);
        }

        protected void paintOn(Graphics g0, Rectangle containerAbsoluteRect) {
            /**
             * !NOTICE
             * thumb.getBounds() is relative to XContentPane, not Track, so,
             * should recompute the bounds relative to containerAbsoluteRect
             */
            Rectangle thumbAbsoluteRect = thumb.getBounds();
            bufRect.setRect(thumbAbsoluteRect.x - containerAbsoluteRect.x, thumbAbsoluteRect.y, thumbAbsoluteRect.width, thumbAbsoluteRect.height);

            Graphics2D g = (Graphics2D) g0;
            Composite backupComposite = g.getComposite();
            g.setColor(LookFeel.getCurrent().getThumbColor());

            /** draw thumb */
            g.setComposite(thumbComposite);
            g.fill(bufRect);

            /** draw thumb sides */
            g.setComposite(arrowComposite);
            g.draw(bufRect);

            if (backupComposite != null) {
                g.setComposite(backupComposite);
            }
        }
    }

    /**
     * Listener for timer events.
     */
    private class ScrollTimerListener implements ActionListener {

        private double numberWithDirection = +1;
        private boolean useBlockIncrement;

        public ScrollTimerListener() {
            this.numberWithDirection = +1;
            this.useBlockIncrement = false;
        }

        public ScrollTimerListener(double nUnitsWithDirection, boolean useBlockIncrement) {
            this.numberWithDirection = nUnitsWithDirection;
            this.useBlockIncrement = useBlockIncrement;
        }

        public void setNumberWithDirection(double nUnitsWithDirection) {
            this.numberWithDirection = nUnitsWithDirection;
        }

        public void setUseBlockIncrement(boolean useBlockIncrement) {
            this.useBlockIncrement = useBlockIncrement;
        }

        public void startScrollTimerIfNecessary() {
            if (scrollTimer.isRunning()) {
                return;
            }

            scrollTimer.start();
        }

        public void actionPerformed(ActionEvent e) {
            if (useBlockIncrement) {
                thumb.scrollByBlock(numberWithDirection);

                /** Stop scrolling if the thumb catches up with the mouse */
                Position mousePressedPosition = getMousePressedPosition();
                if ((numberWithDirection > 0 && mousePressedPosition != Position.AfterThumb) ||
                        (numberWithDirection < 0 && mousePressedPosition != Position.BeforeThumb)) {

                    ((Timer) e.getSource()).stop();
                }
            } else {
                thumb.scrollByUnit(numberWithDirection);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (myMouseAdapter != null) {
            removeMouseListener(myMouseAdapter);
            removeMouseMotionListener(myMouseAdapter);
        }

        super.finalize();
    }

    /**
     * @param nUnitsWithdirection, the number of units that scrolled with positive/negative
     *        diretion, it may not be integer, because the dragging action may
     *        scroll fractional units
     * @TODO The following abstract method can be easily re-written to event/listener
     * -------------------------------------------------------------------
     */
    protected abstract void viewScrolledByUnit(double nUnitsWithdirection);

    protected abstract void viewScaledToRange(double valueShownRange);
}



