package fourquant.arrays

import breeze.linalg.DenseVector
import org.scalatest.{FunSuite, Matchers}

/**
 * Basic tests to ensure I am using the 1D and 2D fft commands correctly
 * Created by mader on 4/24/15.
 */
class VectorTests extends FunSuite with Matchers {
  import breeze.signal._

  def arrStats(mfilt: Array[Double]) = {
    val mean = mfilt.sum/mfilt.length
    val vari = mfilt.map(Math.pow(_,2)).sum-Math.pow(mean,2)
    (mean,vari)
  }
  test("Fourier transform a complex vector") {
    val dv = DenseVector((1 to 100).map(_.toDouble).toArray) //.map(Complex(_,0)))
    val ffdv = fourierTr(dv)
    val ifv = iFourierTr(ffdv)
    ifv.map(_.imag.abs).fold(0.0)((a: Double,b: Double) => a+b) shouldBe 0.0+-1e-5

    ifv.map(_.real).toArray.zip(dv.toArray).foreach{
      case (orig,iif) =>
        orig shouldBe iif+-1e-5
    }
  }

  test("Median filter a vector") {
    val dv = DenseVector((1 to 100).map(x=> x.toDouble+10*Math.pow(-1,x)).toArray)
    val mfilt = breeze.signal.filterMedian(dv,3)

    // variance should be less after the filter
    arrStats(mfilt.toArray)._2 shouldBe < (arrStats(dv.toArray)._2)

  }




}
