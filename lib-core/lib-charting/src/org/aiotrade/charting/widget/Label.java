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
package org.aiotrade.charting.widget;

import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JTextField;
import org.aiotrade.charting.widget.Label.Model;
import org.aiotrade.util.swing.action.EditAction;

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 29, 2006, 1:58 PM
 * @since   1.0.4
 */
public class Label extends AbstractWidget<Model> {
    public final static class Model implements WidgetModel {
        float x;
        float y;
        String text = "Click me to edit";
        
        public void set(float x, float y, String text) {
            this.x = x;
            this.y = y;
            this.text = text; 
        }
        
        public void setText(String text) {
            this.text = text; 
        }
        
        public void set(float x, float y) {
            this.x = x;
            this.y = y; 
        } 
    }
    
    private BufferedImage scratchBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    
    private Font font;
    
    public Label() {
        super();
        addAction(new LabelEditAction());
    }
    
    protected Model createModel() {
        return new Model();
    }
    
    public void setFont(Font font) {
        this.font = font;
    }
    
    public Font getFont() {
        return font;
    }
    
    @Override
    protected Rectangle makePreferredBounds() {
        /**
         * To get the right bounds/size if the text, we need to have a FontRenderContext.
         * And that context has to come from the Graphics2D object, which will come
         * from graphics, that we haven't known yet! A bit of a chicken-and-egg
         * problem. The solution is to create a scratch buffer just to get the
         * FontRenderContext. Then call Font.getStringBounds() to get the size of
         * the text and draw it later.
         */
        final Model model = model();
        final Graphics2D g = scratchBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final FontRenderContext frc = g.getFontRenderContext();
        final Rectangle strBounds = getFont().getStringBounds(model.text, frc).getBounds();
        g.dispose();
        
        /** x, y is the baseline of string, we need to shift the top-left of bounds */
        return new Rectangle(
                (int)(model.x - 1), (int)(model.y - 1) - strBounds.height,
                strBounds.width < 5 ? 5 : strBounds.width + 2, strBounds.height + 2);
    }
    
    protected boolean widgetIntersects(double x, double y, double width, double height) {
        return getBounds().intersects(x, y, width, height);
    }
    
    @Override
    protected boolean widgetContains(double x, double y, double width, double height) {
        return getBounds().contains(x, y, width, height);
    }
    
    protected void plotWidget() {
    }
    
    public void renderWidget(Graphics g0) {
        final Graphics2D g = (Graphics2D)g0;
        
        g.setColor(getForeground());
        g.setFont(getFont());
        Object backupRendingHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        final Model model = model();
        g.drawString(model.text, model.x, model.y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, backupRendingHint);
    }
    
    
    
    public class LabelEditAction extends EditAction {
        private Container container;
        private JTextField textField;
        
        public void execute() {
            final JTextField textField = (JTextField)getEditorPresenter();
            textField.setFont(Label.this.getFont());
            final Rectangle bounds = Label.this.getBounds();
            textField.setText(Label.this.model().text);
            textField.selectAll();
            textField.setVisible(true);
            textField.setBounds(
                    bounds.x, bounds.y,
                    bounds.width < 80 ? 80 : bounds.width + 20, bounds.height);
            textField.grabFocus();
        }
        
        public void anchorEditor(Container container) {
            this.container = container;
            container.add(getEditorPresenter());
        }
        
        private JComponent getEditorPresenter() {
            if (textField == null) {
                textField = new JTextField();
                textField.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Label.this.model().setText(textField.getText());
                        textField.setVisible(false);
                        container.remove(getEditorPresenter());
                        container = null;
                    }
                });
            }
            
            return textField;
        }
    }
    
}

