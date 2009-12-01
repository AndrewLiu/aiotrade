package org.aiotrade.lib.util.swing

import java.awt.GridBagConstraints
import java.awt.Insets

/**
 * This class simplifies the use of the GridBagConstraints class.
 * @version 1.01 2004-05-06
 * @author Cay Horstmann
 */
/**
 * Constructs a GBC with given gridx, gridy, gridwidth, gridheight and all
 * other grid bag constraint values set to the default.
 * @param gridx the gridx position
 * @param gridy the gridy position
 * @param gridwidth the cell span in x-direction
 * @param gridheight the cell span in y-direction
 */
class GBC(agridx: Int, agridy: Int, agridwidth: Int, agridheight: Int) extends GridBagConstraints {
  gridx = agridx
  gridy = agridy

  if (agridwidth > 0) {
    gridwidth = agridwidth
  }

  if (agridheight > 0) {
    gridheight = agridheight
  }
  /**
   * Constructs a GBC with a given gridx and gridy position and all other grid
   * bag constraint values set to the default.
   * @param gridx the gridx position
   * @param gridy the gridy position
   */
  def this(gridx: Int, gridy: Int) = {
    this(gridx, gridy, -1, -1)
  }

  /**
   * Sets the anchor.
   * @param anchor the anchor value
   * @return this object for further modification
   */
  def setAnchor(anchor: Int): GBC = {
    this.anchor = anchor
    this
  }

  /**
   * Sets the fill direction.
   * @param fill the fill direction
   * @return this object for further modification
   */
  def setFill(fill: Int): GBC = {
    this.fill = fill
    this
  }

  /**
   * Sets the cell weights.
   * @param weightx the cell weight in x-direction
   * @param weighty the cell weight in y-direction
   * @return this object for further modification
   */
  def setWeight(weightx: Double, weighty: Double): GBC = {
    this.weightx = weightx
    this.weighty = weighty
    this
  }

  /**
   * Sets the insets of this cell.
   * @param distance the spacing to use in all directions
   * @return this object for further modification
   */
  def setInsets(distance: Int): GBC = {
    this.insets = new Insets(distance, distance, distance, distance)
    this
  }

  /**
   * Sets the insets of this cell.
   * @param top the spacing to use on top
   * @param left the spacing to use to the left
   * @param bottom the spacing to use on the bottom
   * @param right the spacing to use to the right
   * @return this object for further modification
   */
  def setInsets(top: Int, left: Int, bottom: Int, right: Int): GBC = {
    this.insets = new Insets(top, left, bottom, right)
    this
  }

  /**
   * Sets the internal padding
   * @param ipadx the internal padding in x-direction
   * @param ipady the internal padding in y-direction
   * @return this object for further modification
   */
  def setIpad(ipadx: Int, ipady: Int): GBC = {
    this.ipadx = ipadx
    this.ipady = ipady
    this
  }
}
