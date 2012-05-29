package org.aiotrade.lib.math.stats

/**
 * @author Caoyuan Deng
 */
class GeneralizedLeastSquares(x: Array[Double], y: Array[Double], weight: Array[Double], order: Int) {
  assert(x != null && y != null && weight != null && x.length >= 2 && 
         x.length == y.length && x.length == weight.length && order >= 2, 
         "Invald params")
  
  private var _coefficients: Array[Double] = _
  
  def this(x: Array[Double], y: Array[Double], order: Int) = {
    this(x, y, Array.fill[Double](x.length)(1.0), order)
  }

  def coefficients: Array[Double] = {
    if (_coefficients == null) {
      compute()
    }
    _coefficients
  }

  def fit(x: Double): Double = {
    if (_coefficients == null)
      compute()
    if (_coefficients == null)
      return 0
    
    var y = 0.0
    var i = 0
    while (i < _coefficients.length) {
      y += math.pow(x, i) * _coefficients(i)
      i += 1
    }
    y
  }

  private def compute() {
    val s = Array.ofDim[Double]((order - 1) * 2 + 1)
    var i = 0
    while (i < s.length) {
      var j = 0
      while (j < x.length) {
        s(i) += math.pow(x(j), i) * weight(j)
        j += 1
      }
      i += 1
    }
    
    val f = Array.ofDim[Double](order)
    i = 0
    while (i < f.length) {
      var j = 0
      while (j < x.length) {
        f(i) += math.pow(x(j), i) * y(j) * weight(j)
        j += 1
      }
      i += 1
    }
    
    val a = Array.ofDim[Double](order, order)
    i = 0
    while (i < order) {
      var j = 0
      while (j < order) {
        a(i)(j) = s(i + j)
        j += 1
      }
      i += 1
    }
    
    _coefficients = GeneralizedLeastSquares.multiLinearEquationGroup(a, f)
  }
}

object GeneralizedLeastSquares {
  
  /**
   * @param left factors
   * @param right outputs
   * @return result
   */
  def multiLinearEquationGroup(factors: Array[Array[Double]], b: Array[Double]): Array[Double] = {
    val result = Array.ofDim[Double](factors.length)
		
    val len = factors.length - 1
    if (len == 0){
      result(0) = b(0) / factors(0)(0)
      return result
    }
		
    val as = Array.ofDim[Double](len, len)
    val bs = Array.ofDim[Double](len)
    var xIdx = -1
    var yIdx = -1
    var i = 0
    var break_i = false
    while (i <= len && !break_i){
      var j = 0
      var break_j = false
      while (j <= len && !break_j) {
        if (factors(i)(j) != 0.0) {
          yIdx = j
          break_j = true
        }
        j += 1
      }
      if (yIdx != -1){
        xIdx = i
        break_i = true
      }
      i += 1
    }
    
    if (xIdx == -1){
      return null
    }
		
    var count_i = 0
    i = 0
    while (i <= len) {
      if (i != xIdx) {
        bs(count_i) = b(i) * factors(xIdx)(yIdx) - b(xIdx) * factors(i)(yIdx)
        var count_j = 0
        var j = 0
        while (j <= len){
          if (j != yIdx) {
            as(count_i)(count_j) = factors(i)(j) * factors(xIdx)(yIdx) - factors(xIdx)(j) * factors(i)(yIdx)
            count_j += 1
          }        
          j += 1
        }
        count_i += 1
      }
      i += 1
    }
		
    val result2 = multiLinearEquationGroup(as, bs)
		
    var sum = b(xIdx)
    count_i = 0
    i = 0
    while (i <= len){
      if (i != yIdx) {
        sum -= factors(xIdx)(i) * result2(count_i)
        result(i) = result2(count_i)
        count_i += 1
      }
      i += 1
    }
    result(yIdx) = sum / factors(xIdx)(yIdx)
		
    result
  }
  
  // --- simple test
  def main(args: Array[String]) {
    val gls = new GeneralizedLeastSquares(
      Array(2000,   2001,  2002, 2003,  2004,   2005,   2006,   2007,   2008), 
      Array(37.84, 44.55, 45.74, 63.8, 76.67, 105.59, 178.48, 355.27, 409.92), 
      Array(11, 12, 13, 14, 15, 16, 17, 18, 19),
      2
    )
    val xs = gls.coefficients
    println(xs.mkString(","))
    println(gls.fit(2009))
  }

  // --- simple test
  def testMultiLinear(args: Array[String]) {
    val r = multiLinearEquationGroup(
      Array(Array(1,2,3,1), Array(2,0,1,0), Array(5,2,0,0), Array(7,1,1,0)),
      Array(18,5,9,12)
    )
    println(r.mkString(","))
  }
  
}

