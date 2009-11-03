/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.collection

import org.aiotrade.lib.util.collection.ArrayList
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert._

class ArrayListTest {

  @Before
  def setUp: Unit = {
  }

  @After
  def tearDown: Unit = {
  }

  @Test
  def example = {
    val a = ArrayList[Float]()
    val b = ArrayList[Float]()
    b += 1f
    b += 2f
    a.insertAll(0, b)
    a.insert(0, 3f)

    a ++= b
    a foreach println

    a(2) = 10f

    println(a(2))

    a.insertAll(0, Array(11f, 12f))
  }
}
