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
package org.aiotrade.lib.charting.widget

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.util.Collection
import javax.swing.Action
import scala.collection.mutable.ArrayBuffer
import org.aiotrade.lib.charting.util.PathPool
import scala.collection.mutable.{HashMap}

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:34 AM
 * @since   1.0.4
 */
object AbstractWidget {
  protected val pathPool = new PathPool(10, 10, 1000)
  protected val HIT_TEST_SQUARE_RADIUS = 2
}

abstract class AbstractWidget extends Widget {
  import AbstractWidget._
    
  protected def borrowPath: GeneralPath = {
    pathPool.borrowObject
  }
    
  protected def returnPath(path: GeneralPath): Unit = {
    pathPool.returnObject(path)
  }
    
  protected def returnBorrowedPaths(paths: Collection[GeneralPath]): Unit = {
    val itr = paths.iterator
    while (itr.hasNext) {
      pathPool.returnObject(itr.next)
    }
  }
    
  private var opaque: Boolean = _
  private var foreground: Color = Color.WHITE
  private var background: Paint = _
    
  private var location: Point = _
  private var bounds: Rectangle = _
    
  private var _model: M = _
    
  private var renderColorsWithPathBuf: HashMap[Color, GeneralPath] = _
  private var actions: ArrayBuffer[Action] = _
    
  var children: ArrayBuffer[Widget] = _

  def isOpaque: Boolean = {
    opaque
  }
    
  def setOpaque(opaque: Boolean): Unit = {
    this.opaque = opaque
  }
    
  def setBackground(paint:Paint): Unit = {
    this.background = paint
  }
    
  def getBackground :Paint = {
    background
  }
    
  def setForeground(color: Color): Unit = {
    this.foreground = color
  }
    
  def getForeground : Color = {
    foreground
  }
    
  def setLocation(point: Point): Unit = {
    setLocation(point.x, point.y)
  }
    
  def setLocation(x: Double, y: Double): Unit = {
    if (this.location == null) {
      this.location = new Point(x.toInt, y.toInt)
    } else {
      this.location.setLocation(x, y)
    }
  }
    
  def getLocation: Point = {
    if (location == null) new Point(0, 0) else new Point(location)
  }
    
  def setBounds(rect:Rectangle): Unit = {
    setBounds(rect.x, rect.y, rect.width, rect.height)
  }
    
  def setBounds(x: Double, y: Double, width: Double, height: Double): Unit = {
    if (this.bounds == null) {
      this.bounds = new Rectangle(x.toInt, y.toInt, width.toInt, height.toInt)
    } else {
      this.bounds.setRect(x, y, width, height)
    }
  }
    
  def getBounds: Rectangle = {
    if (bounds == null) makePreferredBounds else bounds
  }
    
  protected def makePreferredBounds: Rectangle = {
    val childrenBounds = new Rectangle
    if (children != null) {
      for (child <- children) {
        childrenBounds.add(child.getBounds)
      }
    }
        
    childrenBounds
        
    /** @TODO */
  }
    
  def contains(point:Point): Boolean = {
    contains(point.x, point.y)
  }
    
  def contains(x: Double, y: Double): Boolean = {
    contains(x, y, 1, 1)
  }
    
  def contains(rect:Rectangle): Boolean = {
    contains(rect.x, rect.y, rect.width, rect.height)
  }
    
  def contains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (isOpaque) {
      getBounds.intersects(x, y, width, height)
    } else {
      if (isContainerOnly) {
        childrenContain(x, y, width, height)
      } else {
        widgetContains(x, y, width, height) || childrenIntersect(x, y, width, height)
      }
    }
  }
    
  protected def widgetContains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    return getBounds.contains(x, y, width, height);
  }
    
  protected def childrenContain(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (children != null) {
      for (child <- children) {
        if (child.contains(x, y, width, height) ) {
          return true
        }
      }
    }
    return false
  }
    
  def intersects(rect:Rectangle): Boolean = {
    intersects(rect.x, rect.y, rect.width, rect.height)
  }
    
  def intersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (isOpaque) {
      getBounds.intersects(x, y, width, height)
    } else {
      if (isContainerOnly) {
        childrenIntersect(x, y, width, height)
      } else {
        widgetIntersects(x, y, width, height) || childrenIntersect(x, y, width, height)
      }
    }
  }
    
  protected def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean
    
  protected def childrenIntersect(x: Double, y: Double, width: Double, height: Double): Boolean = {
    if (children != null) {
      for (child <- children) {
        if (child.intersects(x, y, width, height) ) {
          return true
        }
      }
    }
    return false
  }
    
  def hits(point: Point): Boolean = {
    hits(point.x , point.y)
  }
    
  def hits(x: Double, y: Double): Boolean = {
    intersects(
      x - HIT_TEST_SQUARE_RADIUS, y - HIT_TEST_SQUARE_RADIUS,
      2 * HIT_TEST_SQUARE_RADIUS, 2 * HIT_TEST_SQUARE_RADIUS)
  }
    
  def model: M = {
    if (_model == null) {
      _model = createModel
    }
        
    _model
  }
    
  protected def createModel: M
    
  def plot: Unit = {
    reset
    plotWidget
  }
    
  protected def plotWidget: Unit
    
  def render(g0: Graphics): Unit = {
    val g = g0.asInstanceOf[Graphics2D]
        
    val location = getLocation
    val backupTransform = g.getTransform
    if (!(location.x == 0 && location.y == 0)) {
      g.translate(location.x, location.y)
    }
        
    val bounds = getBounds
    val backupClip = g.getClip
    g.clip(bounds)

    val clipBounds = g.getClipBounds
    if (intersects(clipBounds) || clipBounds.contains(bounds) || bounds.height == 1 || bounds.width == 1) {
      if (isOpaque) {
        g.setPaint(getBackground)
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
      }
            
      renderWidget(g0)
      renderChildren(g0)
    }
        
    g.setClip(backupClip)
    g.setTransform(backupTransform)
  }
    
  protected def renderWidget(g: Graphics)
    
  protected def renderChildren(g0: Graphics) {
    if (children == null) {
      return
    }
        
    val g = g0.asInstanceOf[Graphics2D]
        
    val clipBounds = g.getClipBounds
    for (child <- children) {
      if (child.intersects(clipBounds) || clipBounds.contains(child.getBounds) || child.getBounds.height == 1 || child.getBounds.width == 1) {
        child match {
          case x: PathWidget =>
            if (renderColorsWithPathBuf == null) {
              renderColorsWithPathBuf = new HashMap[Color, GeneralPath]
            }
            val color = child.getForeground
            val renderPathBuf = renderColorsWithPathBuf.get(color) getOrElse {
              val renderPathBufx = borrowPath
              renderColorsWithPathBuf.put(color, renderPathBufx)
              renderPathBufx
            }
                
            val path = x.getPath
            var shape :Shape = path
                
            val location = child.getLocation
            if ( (location.x == 0 && location.y == 0)) {
              val transform = AffineTransform.getTranslateInstance(location.x, location.y)
              shape = path.createTransformedShape(transform)
            }
                
            renderPathBuf.append(shape, false)
          case _ => child.render(g)
        }
      }
    }
        
    if (renderColorsWithPathBuf != null) {
      for (color <- renderColorsWithPathBuf.keysIterator) {
        renderColorsWithPathBuf.get(color) match {
          case None =>
          case Some(path) =>
            g.setColor(color)
            g.draw(path)
            returnPath(path)
        }
      }
      renderColorsWithPathBuf.clear
    }
  }
    
  /** override it if only contains children (plotWidget() do noting) */
  def isContainerOnly: Boolean = {
    false
  }
    
  def addChild[T <: Widget](child: T) :T = {
    if (children == null) {
      children = new ArrayBuffer[Widget]
    }
        
    children += child
    child
  }
    
  def removeChild(child: Widget): Unit = {
    if (children != null) {
      children.remove(children.indexOf(child))
    }
  }
    
  def getChildren: ArrayBuffer[Widget] = {
    if (children != null) {
      children
    } else new ArrayBuffer[Widget]
  }
    
  def resetChildren: Unit = {
    if (children != null) {
      for (child <- children) {
        child.reset
      }
    }
  }
    
  def clearChildren: Unit = {
    if (children != null) {
      children.clear
    }
  }
    
  def lookupChildren[T <: Widget: Manifest](widgetType: Class[T], foreground: Color): ArrayBuffer[T] = {
    val result = new ArrayBuffer[T]
    if (children != null) {
      for (child <- children) {
        if (widgetType.isInstance(child) && child.getForeground.equals(foreground)) {
          result += child.asInstanceOf[T]
        }
      }
    }
    result
  }
    
  def lookupFirstChild[T <: Widget](widgetType: Class[T], foreground: Color): Option[T] = {
    if (children != null) {
      for (child <- children) {
        if (widgetType.isInstance(child) && child.getForeground.equals(foreground)) {
          return Some(child.asInstanceOf[T])
        }
      }
    }
    None
  }
    
  def addAction(action: Action): Action = {
    if (actions == null) {
      actions = new ArrayBuffer[Action]
    }
        
    actions += action
    action
  }
    
  def lookupAction[T <: Action](tpe: Class[T]): Option[T] = {
    if (actions != null) {
      for (action <- actions) {
        if (tpe.isInstance(action)) {
          return Some(action.asInstanceOf[T])
        }
      }
    }
    None
  }
    
  def lookupActionAt[T <: Action](tpe: Class[T], point: Point): Option[T] = {
    /** lookup children first */
    if (children != null) {
      for (child <- children) {
        if (child.contains(point)) {
          return child.lookupAction(tpe)
        }
      }
    }

    /** then lookup this */
    if (getBounds.contains(point)) lookupAction(tpe) else None
  }
    
  def reset: Unit = {
    if (children != null) {
      for (child <- children) {
        child.reset
      }
      children.clear
    }
  }

}
