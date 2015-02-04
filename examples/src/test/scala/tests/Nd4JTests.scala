package tests

import org.nd4j.linalg.factory.Nd4j
import org.scalatest.FunSuite
import org.scalatest.Matchers._

/**
 * Created by mader on 2/3/15.
 */
class Nd4JTests extends FunSuite {
  val arr1 = Nd4j.create(Array[Float](1,2,3,4),Array(2,2))

  test("Setup ND4J") {
    println(arr1)
    arr1.getInt(0,0) shouldBe 1
    arr1.getInt(1,1) shouldBe 4
  }

  test("Adding") {
    arr1.addi(1)
    println(arr1)
    arr1.getInt(0,0) shouldBe 2
    arr1.getInt(1,1) shouldBe 5
  }

  test("Adding arrays") {
    val arr2 = Nd4j.create(Array[Float](4,3,2,1),Array(2,2))
    arr1.addi(arr2)
    println(arr1)
    arr1.getInt(0,0) shouldBe 5
    arr1.getInt(1,1) shouldBe 5
  }

}
