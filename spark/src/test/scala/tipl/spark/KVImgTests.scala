package tipl.spark

import java.io.File

import com.google.common.io.Files
import org.scalatest.FunSuite
import org.scalatest._
import Matchers._
import tipl.spark.KVImgOps._
import tipl.tests.LocalSparkContext
import tipl.util.D3int
import tipl.util.TIPLOps._

/**
 * Created by mader on 10/10/14.
 */
class KVImgTests extends FunSuite with LocalSparkContext {

  var tempDir: File = _

  override def beforeEach() {
    super.beforeEach()
    tempDir = Files.createTempDir()
    tempDir.deleteOnExit()
  }

  override def afterEach() {
    super.afterEach()
    tempDir.recursiveDelete()
  }

  test("Check if we even get a sparkcontext") {
    sc = getSpark("Acquiring context...")
    println("Current context is named: " + sc.appName)
  }

  test("KVIO >") {
    sc = getSpark("KVImgOps")
    val kv = sc.parallelize(1 to 100).
      map { i => (new D3int(i), i)}
    print("Current keys:" + kv.first)
    assert(kv.count() == 100)

    val kvt = kv > 50
    print("Current keys:" + kvt.first)
    assert(kvt.count() == kv.count())
    assert(kvt.filter(_._2).count() == 50)
    println(kvt.first)
  }

  test("KVIO Subtraction") {
    sc = getSpark("KVImgOps2")

    val kv = sc.parallelize(1 to 100).
      map { i => (new D3int(i), i)}
    val kvd = (kv - 1) < 10
    print("Current keys:" + kvd.first)
    assert(kvd.count() == kv.count())
    assert(kvd.filter(_._2).count() == 10)
  }

  test("Times") {

    val kvnorm = sc.parallelize(1 to 100).
      map { i => (new D3int(i), i.toDouble)}
    val kvinv = sc.parallelize(1 to 100).
      map { i => (new D3int(i), 1 / i.toDouble)}
    val n: NumericRichKvRDD[Double] = NumericRichKvRDD[Double](kvnorm)
    val kvm = (n.times(kvnorm, kvinv)).map(_._2)
    kvm.min.toDouble shouldBe  (1.0+-1e-3)
    kvm.max.toDouble shouldBe (1.0+-1e-3)
  }

}
