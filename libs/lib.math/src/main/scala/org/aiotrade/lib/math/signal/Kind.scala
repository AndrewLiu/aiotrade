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
package org.aiotrade.lib.math.signal

/**
 *
 * @author Caoyuan Deng
 */
object Kind {
  def withId(id: Byte): Kind = {
    if (isSign(id)) Direction.withId(id) else Position.withId(id)
  }

  def isSign(id: Byte): Boolean = id > 0
}

abstract class Kind {def id: Byte}

abstract class Direction(val id: Byte) extends Kind
object Direction {
  case object EnterLong  extends Direction(1)
  case object ExitLong   extends Direction(2)
  case object EnterShort extends Direction(3)
  case object ExitShort  extends Direction(4)

  def withId(id: Byte): Direction = id match {
    case 1 => Direction.EnterLong
    case 2 => Direction.ExitLong
    case 3 => Direction.EnterShort
    case 4 => Direction.ExitShort
  }
}

abstract class Position(val id: Byte) extends Kind
object Position {
  case object Upper extends Position(-1)
  case object Lower extends Position(-2)

  def withId(id: Byte): Position = id match {
    case -1 => Position.Upper
    case -2 => Position.Lower
  }
}


