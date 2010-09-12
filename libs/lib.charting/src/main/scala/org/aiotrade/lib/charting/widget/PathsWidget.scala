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
import java.awt.Rectangle
import java.awt.geom.GeneralPath
import scala.collection.mutable.HashMap


/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 27, 2006, 7:38 AM
 * @since   1.0.4
 */
class PathsWidget extends AbstractWidget {
    
  private val colorToPathPair = new HashMap[Color, (GeneralPath, GeneralPath)]
    
  protected def createModel: M = null
    
  override def isContainerOnly: Boolean = {
    false
  }
    
  override protected def makePreferredBounds: Rectangle = {
    val pathBounds = new Rectangle
    for ((path, pathFilled) <- colorToPathPair.valuesIterator) {
      pathBounds.add(path.getBounds)
    }
        
    return new Rectangle(
      pathBounds.x, pathBounds.y,
      pathBounds.width + 1, pathBounds.height + 1)
  }
    
  def pathOf(color: Color): (GeneralPath, GeneralPath) = {
    colorToPathPair.get(color) match {
      case None =>
        val pathx = borrowPath
        val pathFilled = borrowPath
        colorToPathPair.put(color, (pathx, pathFilled))
        (pathx, pathFilled)
      case Some(x) => x  
    }
  }
    
  def appendFrom(pathWidget: PathWidget) {
    val color = pathWidget.getForeground
    pathOf(color) match {
      case (path, pathFilled) =>
        if (pathWidget.isFilled) {
          pathFilled.append(pathWidget.getPath, false)
        } else {
          path.append(pathWidget.getPath, false)
        }
    }
  }
    
  override protected def widgetContains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    colorToPathPair.valuesIterator exists {pathPair =>
      pathPair._1.contains(x, y, width, height) || pathPair._2.contains(x, y, width, height)
    }
  }
    
  override def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    colorToPathPair.valuesIterator exists {pathPair =>
      pathPair._1.intersects(x, y, width, height) || pathPair._2.intersects(x, y, width, height)
    }
  }
    
  override def renderWidget(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]
        
    for ((color, (path, pathFill)) <- colorToPathPair) {
      g.setColor(color)
      g.draw(path)
      g.draw(pathFill)
    }
  }
    
  override def reset {
    super.reset
    for ((path, pathFill) <- colorToPathPair.valuesIterator) {
      path.reset
      pathFill.reset
    }
  }
    
  override protected def plotWidget {}

  @throws(classOf[Throwable])
  override protected def finalize {
    for ((path, pathFill) <- colorToPathPair.valuesIterator) {
      returnPath(path)
      returnPath(pathFill)
    }
        
    super.finalize
  }
    
}

