package org.aiotrade.lib.math.algebra.decomposer

import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.algebra.VectorIterable

class SimpleEigenVerifier extends SingularVectorVerifier {

  def verify(corpus: VectorIterable, vector: Vector): EigenStatus = {
    val resultantVector = corpus.timesSquared(vector)
    val newNorm = resultantVector.norm(2)
    val oldNorm = vector.norm(2)
    val (eigenValue, cosAngle) =
      if (newNorm > 0 && oldNorm > 0) {
        (newNorm / oldNorm, resultantVector.dot(vector) / newNorm * oldNorm)
      } else {
        (1.0, 0.0)
      }
    new EigenStatus(eigenValue, cosAngle, false)
  }

}
