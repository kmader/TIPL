
/*
 * A few basic tests for Spark
 */

package tipl.tests

import scala.math.random
import org.apache.spark._
import tipl.spark.SparkGlobal

/** Computes an approximation to pi with failing jobs */
object SparkFailureTest {
  def main(args: Array[String]) {
    val p = SparkGlobal.activeParser(args)
    val slices = p.getOptionInt("slices", 100000, "Number of slices for Pi calculation")
    val failureRate = p.getOptionDouble("fail", 1e-3 / 1e3, "rate of failure for spark jobs " +
      "(random exception thrown)")
    p.checkForInvalid()
    val sc = SparkGlobal.getContext("CrashTest")

    val n = 100 * slices
    val exCount = sc.accumulator(0)
    val count = sc.parallelize(1 to n, slices).map { i =>
      val x = random * 2 - 1
      val y = random * 2 - 1
      if (random < failureRate) {
        exCount += 1
        throw new InterruptedException("Random Exception Testing")
      }
      if (x * x + y * y < 1) 1 else 0
    }.reduce(_ + _)
    println("Pi is roughly " + 4.0 * count / n + ", with #" + exCount.value + " exceptions")
    sc.stop()
  }
}
