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
package org.aiotrade.lib.math.vector

import java.util.Random

/**
 * @author Caoyuan Deng
 */
class InputOutputPointSet(iops: Array[InputOutputPoint]) {

  private val inputOutputPoints = iops

  private val inputDimension = iops(0).input.dimension
  private var inputMeans = new Array[Double](inputDimension)
  private var inputStdDeviations = new Array[Double](inputDimension)
  private var inputNormalized = new Array[Boolean](inputDimension)

  private val outputDimension = iops(0).output.dimension
  private var outputMeans = new Array[Double](outputDimension)
  private var outputStdDeviations = new Array[Double](outputDimension)
  private var outputNormalized = new Array[Boolean](outputDimension)
    
  def toArray: Array[InputOutputPoint] = {
    inputOutputPoints
  }
    
  def  apply(idx: Int): InputOutputPoint = {
    inputOutputPoints(idx)
  }
    
  def update(idx: Int, iop: InputOutputPoint): Unit = {
    inputOutputPoints(idx) = iop
  }
    
  def size: Int =  {
    inputOutputPoints.length
  }
    
  def randomizeOrder: Unit =  {
    val size1 = size
        
    val random = new Random(System.currentTimeMillis)
    for (i <- 0 until size1) {
      val next = random.nextInt(size1 - i)
            
      val iop = inputOutputPoints(next)
            
      inputOutputPoints(next) = inputOutputPoints(i)
      inputOutputPoints(i) = iop
    }
  }
    
  def cloneWithRandomizedOrder: InputOutputPointSet = {
    val size1 = size
        
    val newPoints = new Array[InputOutputPoint](size1)
    System.arraycopy(inputOutputPoints, 0, newPoints, 0, size)
        
    val newSet = new InputOutputPointSet(newPoints)
        
    newSet.inputMeans = new Array[Double](inputMeans.length)
    System.arraycopy(inputMeans, 0, newSet.inputMeans, 0, inputMeans.length)
        
    newSet.inputStdDeviations = new Array[Double](inputStdDeviations.length)
    System.arraycopy(inputStdDeviations, 0, newSet.inputStdDeviations, 0, inputStdDeviations.length)
        
    newSet.outputMeans = new Array[Double](outputMeans.length)
    System.arraycopy(outputMeans, 0, newSet.outputMeans, 0, outputMeans.length)
        
    newSet.outputStdDeviations = new Array[Double](outputStdDeviations.length)
    System.arraycopy(outputStdDeviations, 0, newSet.outputStdDeviations, 0, outputStdDeviations.length)
        
    newSet.randomizeOrder
        
    newSet
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   */
  def normalizeInputs(dimensionIdx: Int): Unit = {
    val num = inputOutputPoints.length
        
    val values = new Array[Double](num)
    for (i <- 0 until num) {
      values(i) = inputOutputPoints(i).input(dimensionIdx)
    }
        
    val normalized = normalize_ZScore(values)
        
    for (i <- 0 until num) {
      inputOutputPoints(i).input(dimensionIdx) = normalized(i)
    }
        
    inputMeans(dimensionIdx) = normalized(num)
    inputStdDeviations(dimensionIdx) = normalized(num + 1)
    inputNormalized(dimensionIdx) = true
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   *
   * @NOTICE
   * If the output layer uses linear neurons as y = x, the y will be 0 symmetry.
   * the output can be < 0 in same probabilty as > 0, so we should also normalize
   * outputs to [-1, 1] instead of positively?
   *
   * 1. If the hidden neurons' outputs are positive-polarity (such as: LogiSigmoidNeuron)
   * when the mean of initial weights is about 0, the output will be around 0.5,
   * so, we'd better to normalize the outputs to [0, 1], or, with 0.5 mean and 0.5 stdDeviation
   *
   * 2. If the hidden neurons' outputs are double-polarity (such as: TanhSigmoidNeuron)
   * when the mean of initial weights is about 0, the output will be around 0,
   * so, we'd better to normalize the outputs to [-1, 1], or, with 0 mean and 1 stdDeviation
   *
   * Experience: If normalize ouput to [-1, 1], will cause a slower convergence.
   */
  def normalizeOutputs(dimensionIdx: Int): Unit = {
    val num = inputOutputPoints.length
        
    val values = new Array[double](num)
    for (i <- 0 until num) {
      values(i) = inputOutputPoints(i).output(dimensionIdx)
    }
        
    val normalized = normalize_ZScore(values)
        
    for (i <- 0 until num) {
      inputOutputPoints(i).output(dimensionIdx) = normalized(i)
    }
        
    val mu = normalized(num)
    val sigma = normalized(num + 1)
    outputMeans(dimensionIdx) = normalized(num)
    outputStdDeviations(dimensionIdx) = normalized(num + 1)
    outputNormalized(dimensionIdx) = true
  }
    
  /**
   * Normalize values to:
   *   mean: 0.5
   *   standard deviation: 0.5
   *   range: about [0, 1]
   */
  def normalizeOutputsPositively(dimensionIdx: Int): Unit = {
    val num = inputOutputPoints.length
        
    val values = new Array[double](num)
    for (i <- 0 until num) {
      values(i) = inputOutputPoints(i).output(dimensionIdx)
    }
        
    val normalized = normalize_ZScore(values);
        
    for (i <- 0 until num) {
      /** transform to mean: 0.5, standar deviation: 0.5 */
      inputOutputPoints(i).output(dimensionIdx) = normalized(i) * 0.5 + 0.5
    }
        
    /**
     * When doing normalize_ZScore(),
     *   y = (x - mu) / sigma
     * Here, we again,
     *   v = y * 0.5 + 0.5
     * So,
     *   v = ((x - mu) / sigma) * 0.5 + 0.5
     *     = ((x - mu) + 0.5 * sigma / 0.5) / (sigma / 0.5)
     *     = (x - (mu - sigma)) / (sigma / 0.5)
     *     = (x - mu') / sigma'
     * where
     *   mu' = mu - sigma
     *   sigma' = sigma / 0.5
     */
    val mu = normalized(num)
    val sigma = normalized(num + 1)
    outputMeans(dimensionIdx) = mu - sigma
    outputStdDeviations(dimensionIdx) = sigma / 0.5
    outputNormalized(dimensionIdx) = true
  }
    
  def normalizeAllInputs: Unit = {
    for (i <- 0 until apply(0).input.dimension) {
      normalizeInputs(i)
    }
  }
    
  def normalizeAllOutputs: Unit = {
    for (i <- 0 until apply(0).output.dimension) {
      normalizeOutputs(i)
    }
  }
    
  def normalizePositivelyAllOutputs: Unit = {
    for (dimension <- 0 until apply(0).output.dimension) {
      normalizeOutputsPositively(dimension)
    }
  }
    
  def normalizeInput(input: Vec): Unit = {
    for (i <- 0 until input.dimension) {
      val value = input(i)
      input(i) = normalizeInput(i, value)
    }
  }
    
  def normalizeOutput(output: Vec): Unit = {
    for (i <- 0 until output.dimension) {
      val value = output(i)
      output(i) = normalizeOutput(i, value)
    }
  }
    
  def normalizePositivelyOutput(output: Vec): Unit =  {
    /** as we have considered the mean and stdDeviation in positive case, it's same as: */
    normalizeOutput(output)
  }
    
  def  normalizeInput(dimensionIdx: Int, value: Double): Double = {
    if (inputNormalized(dimensionIdx)) {
      (value - inputMeans(dimensionIdx)) / inputStdDeviations(dimensionIdx)
    } else {
      /** the mean and stdDeviation of this dimensionIdx are not computed yet */
      value
    }
  }
    
  def normalizeOutput(dimensionIdx: Int, value: Double): Double = {
    if (outputNormalized(dimensionIdx)) {
      (value - outputMeans(dimensionIdx)) / outputStdDeviations(dimensionIdx)
    } else {
      /** the mean and stdDeviation of this dimensionIdx are not computed yet */
      value
    }
  }
    
  def normalizePositivelyOutput(dimensionIdx: Int, value: Double): Double = {
    /** as we have considered the mean and stdDeviation in positive case, it's same as: */
    normalizeOutput(dimensionIdx, value)
  }
    
  def revertInput(input: Vec): Unit = {
    for (i <- 0 until input.dimension) {
      var value = input(i)
            
      value = value * inputStdDeviations(i) + inputMeans(i)
      input(i) = value
    }
  }
    
  def revertOutput(output: Vec): Unit = {
    for (i <- 0 until output.dimension) {
      var value = output(i)
            
      value = value * outputStdDeviations(i) + outputMeans(i)
      output(i) = value
    }
  }
    
    
  def revertInput(dimensionIdx: Int, value: Double): Double = {
    value * inputStdDeviations(dimensionIdx) + inputMeans(dimensionIdx)
  }
    
    
  def revertOutput(dimensionIdx: Int, value: Double): Double = {
    value * outputStdDeviations(dimensionIdx) + outputMeans(dimensionIdx)
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   */
  private def normalize_ZScore(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** compute mean value */
    var sum = 0d
    for (i <- 0 until num) {
      sum += values(i)
    }
    val mean = sum / (num * 1d)
        
    /** compute standard deviation */
    var deviation_square_sum = 0d
    for (i <- 0 until num) {
      val deviation = values(i) - mean
      deviation_square_sum += deviation * deviation
    }
        
    var stdDeviation = Math.sqrt(deviation_square_sum / (num * 1d))
        
    System.out.println("Mean: " + mean + " Standard Deviation: " + stdDeviation)
        
    if (stdDeviation == 0) {
      stdDeviation = 1
    }
        
    /**
     * do 'Z Score' normalization.
     * 2 more dimensions are added to store mean and stdDeviation
     */
    val normalized = new Array[double](num + 2)
    for (i <- 0 until num) {
      normalized(i) = (values(i) - mean) / stdDeviation
    }
        
    normalized(num) = mean
    normalized(num + 1) = stdDeviation
        
    normalized
  }
    
  /**
   * y = (0.9 - 0.1) / (xmax - xmin) * x + (0.9 - (0.9 - 0.1) / (xmax - xmin) * xmax)
   *   = 0.8 / (xmax - xmin) * x + (0.9 - 0.8 / (xmax - xmin) * xmax)
   */
  private def normalizePositively_MinMax(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** compute min max value */
    var min = +Double.MaxValue
    var max = -Double.MaxValue
    for (i <- 0 until num) {
      val value = values(i)
      if (value < min) {
        min = value
      }
      if (value > max) {
        max = value
      }
    }
        
    val mean = min
        
    val stdDeviation = max - min
        
    System.out.println("normOutput: " + mean + " deviationOutput: " + stdDeviation)
        
    /** do 'min max' normalization */
    val normalized = new Array[Double](num + 2)
    for (i <- 0 until num) {
      normalized(i) = (values(i) - mean) / stdDeviation
    }
        
    normalized(num) = mean
    normalized(num + 1) = stdDeviation
        
    normalized
  }
    
  private def normalizePositively_CustomMinMax(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** compute max min value */
    val max = 30000
    val min = 0
        
    val mean = min
        
    val stdDeviation = max - min
        
    /** do 'maxmin' standardization */
    val normalized = new Array[Double](num + 2)
    for (i <- 0 until num) {
      normalized(i) = (values(i) - mean) / stdDeviation
    }
        
    normalized(num) = mean
    normalized(num + 1) = stdDeviation
        
    normalized
  }
}
