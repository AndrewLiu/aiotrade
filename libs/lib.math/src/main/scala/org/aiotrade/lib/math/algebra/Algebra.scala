package org.aiotrade.lib.math.algebra

object Algebra {
  def mult(m: Matrix, v: Vector): Vector = {
    if (m.numRows != v.size) {
      throw new CardinalityException(m.numRows, v.size)
    }
    // Use a Dense Vector for the moment,
    val result = DenseVector(m.numRows)
    
    var i = 0
    while (i < m.numRows) {
      result.set(i, m.viewRow(i).dot(v))
      i += 1
    }
    
    result
  }
  
  /** Returns sqrt(a^2 + b^2) without under/overflow. */
  def hypot(a: Double, b: Double): Double = {
    var r = 0.0
    if (math.abs(a) > math.abs(b)) {
      r = b / a
      r = math.abs(a) * math.sqrt(1 + r * r)
    } else if (b != 0) {
      r = a / b
      r = math.abs(b) * math.sqrt(1 + r * r)
    } else {
      r = 0.0
    }
    r
  }
  
  /**
   * Compute Maximum Absolute Row Sum Norm of input Matrix m
   * http://mathworld.wolfram.com/MaximumAbsoluteRowSumNorm.html 
   */
  def getNorm(m: Matrix): Double = {
    var max = 0.0;
    var i = 0
    while (i < m.numRows) {
      var sum = 0;
      val cv = m.viewRow(i)
      var j = 0
      while (j < cv.size) {
        sum += math.abs(cv.getQuick(j)).toInt
        j += 1
      }
      if (sum > max) {
        max = sum
      }
      i += 1
    }
    max
  }
  
  def toArray(A: Matrix): Array[Array[Double]] = {
    checkSquare(A)
    val n = A.numCols
    val V = new Array[Array[Double]](n, n)
    for (slice <- A) {
      val row = slice.index
      for (element <- slice.vector) {
        V(row)(element.index) = element.get
      }
    }
    V
  }

  def isSymmetric(matrix: Array[Array[Double]]): Boolean = {
    var i = 0
    while (i < matrix.length) {
      var j = 0 
      while (j<i) {
        if (matrix(i)(j) != matrix(j)(i)) {
          return false
        }
        j += 1
      }
      i += 1
    }
    true
  }

  def checkSquare(matrix: Matrix) = {
    if(matrix.numRows != matrix.numCols) {
      throw new IllegalArgumentException("Matrix must be square")     
    }
  }

}
