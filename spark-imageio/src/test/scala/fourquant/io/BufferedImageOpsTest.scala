package fourquant.io

import fourquant.io.BufferedImageOps.{implicits, ScaledDoubleImageMapping}
import org.scalatest.{Matchers, FunSuite}

/**
 * Created by mader on 4/17/15.
 */
class BufferedImageOpsTest extends FunSuite with Matchers {
  val sd = new ScaledDoubleImageMapping(0,255)

  test("ScaledImageMapping - Byte Conversion") {

    sd.fromByte(-128) shouldBe 0.0 +- 0.5
    sd.fromByte(0) shouldBe 128.0 +- 0.5
    sd.fromByte(127) shouldBe 255.0 +- 0.5

    sd.toByte(0.0) shouldBe -128

    sd.fromByte(sd.toByte(10)) shouldBe 10.0 +- 0.01
    sd.fromByte(sd.toByte(1000)) shouldBe sd.max +- 0.01
  }

  test("ScaledImageMapping - Int Conversions") {
    sd.toInt(255) shouldBe Int.MaxValue
    sd.fromInt(Int.MaxValue) shouldBe 255

    sd.toInt(0) shouldBe Int.MinValue

    sd.fromInt(sd.toInt(100.0)) shouldBe 100.0 +- 0.01
    // test the limits
    sd.fromInt(sd.toInt(256.0)) shouldBe sd.max +- 0.01
  }

  val nd = implicits.directDoubleImageSupport
  test("DirectImageMapping - Byte Conversion") {
    nd.fromByte(-128) shouldBe -128.0 +- 0.01
    nd.fromByte(0) shouldBe 0.0 +- 0.01
    nd.fromByte(127) shouldBe 127.0 +- 0.5

    nd.toByte(0.0) shouldBe 0

    nd.fromByte(nd.toByte(10)) shouldBe 10.0 +- 0.01
    nd.fromByte(nd.toByte(1000)) shouldBe -24.0 +- 0.5 // maybe you needed a guarded conversion
  }
}
