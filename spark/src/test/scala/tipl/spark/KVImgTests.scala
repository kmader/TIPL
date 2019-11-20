package tipl.spark

import java.io.File

import com.google.common.io.Files
import org.apache.spark.SparkContext
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import spark.images.FilterImpl.GaussianFilter
import tipl.spark.KVImgOps._
import tipl.tests.LocalSparkContext
import tipl.util.TIPLOps._
import tipl.util.{D3float, D3int, TImgTools}

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
      map { i => (new D3int(i), i) }
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
      map { i => (new D3int(i), i) }
    val kvd = (kv - 1) < 10
    print("Current keys:" + kvd.first)
    assert(kvd.count() == kv.count())
    assert(kvd.filter(_._2).count() == 10)
  }

  test("Times") {

    val kvnorm = sc.parallelize(1 to 100).
      map { i => (new D3int(i), i.toDouble) }
    val kvinv = sc.parallelize(1 to 100).
      map { i => (new D3int(i), 1 / i.toDouble) }
    val n: NumericRichKvRDD[Double] = NumericRichKvRDD[Double](kvnorm)
    val kvm = (n.times(kvnorm, kvinv)).map(_._2)
    kvm.min.toDouble shouldBe (1.0 +- 1e-3)
    kvm.max.toDouble shouldBe (1.0 +- 1e-3)
  }

  def testKVImage(sc: SparkContext, maxCnt: Int = 10) = {
    sc.parallelize(0 to maxCnt).
      map { i => (new D3int(3 * i, 3 * i, 3 * i), i.toDouble) }.
      flatMap { i =>
        for (x <- -1 to 1; y <- -1 to 1; z <- -1 to 1) yield (i._1 + new D3int(x, y, z), i._2)
      }
  }

  test("MLLib On Slices") {
    import tipl.spark.SliceableOps._
    val kvnorm = testKVImage(sc, 10)
    val kvobj = new NumericKVImg(new D3int(30), new D3int(0),
      new D3float(1.0f), TImgTools.IMAGETYPE_DOUBLE, kvnorm)
    val dm = kvobj.getZSlice(new D3int(0, 0, 3)) match {
      case Some(idm) =>
        assert(idm.toCM.entries.count == 9, "Total entries are 9")
        assert(idm.toRM.rows.count == 3, "Rows should be 3")
      case _ => fail("Get Z Slice is a valid command")
    }

    kvobj.getNSlice(0, 0, new D3int(0, 0, 0)) match {
      case Some(a) => fail("Matching indices should produce no output")
      case None => Unit
    }
  }
}

class VoxOpsTest extends FunSuite with LocalSparkContext {

  import org.apache.spark.SparkContext._
  import spark.images.VoxOps._

  def pointImage(sc: SparkContext) = {
    val xvals = sc.parallelize(4 to 6)
    xvals.
      cartesian(xvals).
      cartesian(xvals).
      map(pt => (new D3int(pt._1._1, pt._1._2, pt._2), if (pt == ((5, 5), 5)) 1 else 0))
  }

  def makeGaussian(rad: Double) = {
    new VoxelFilter[Int]() with GaussianFilter {
      val radius = rad
    }
  }

  test("Basic Filter") {
    sc = getSpark("KVImgOps")

    val kv = pointImage(sc)
    val kvsBefore = kv.map(_._2).stats

    val nkv = kv(makeGaussian(1.0))
    val kvsAfter = nkv.getBaseImg().map(_._2).stats
    println("Before:" + kvsBefore)
    kvsBefore.max.toDouble shouldBe (1)
    println("After:" + kvsAfter)
    kvsAfter.max.toDouble shouldBe (0.39 +- 1e-3)

  }
  test("Basic Filter Slow Voxel") {
    sc = getSpark("KVImgOps")

    val kv = pointImage(sc)
    val kvsBefore = kv.map(_._2).stats

    val nkv = kv.sapply(makeGaussian(1.0))
    val kvsAfter = nkv.getBaseImg().map(_._2).stats

    println("Before:" + kvsBefore)
    println("After:" + kvsAfter)
    kvsBefore.max.toDouble shouldBe (1)

    kvsAfter.max.toDouble shouldBe (0.39 +- 1e-3)

  }
}
