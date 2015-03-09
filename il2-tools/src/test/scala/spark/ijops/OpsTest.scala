package spark.ijobs

import net.imagej.ImageJ
import net.imagej.ops.Op
import net.imglib2.`type`.numeric.real.DoubleType
import org.scalatest.FunSuite

import scala.collection.JavaConversions._


class OpsTest extends FunSuite {
  val ij= new ImageJ() //classOf[OpService])

  test("ImageJ Setup and Ops") {
    val opCount = ij.command().getCommandsOfType(classOf[Op]).size();
    println("Total Ops:"+opCount)
    assert(opCount>100,"At least 100 operations show up which means everything is setup correctly")
    val opList = ij.command().getCommandsOfType(classOf[Op]).map(_.getTitle)
    assert(opList.contains("add"),"Has an add operation")
    assert(opList.contains("chunker"),"Has a chunker operation")

  }

  test("Create Image from Op and Dimensions") {
    // create a new blank image\
    val dims = Array[Long](150, 100)
    val blank = ij.op().createimg(dims)

    // fill in the image with a sinusoid using a formula

    val formula = "10 * (Math.cos(0.3*p[0]) + Math.sin(0.3*p[1]))"
    /*val sinusoid = ij.op().equation(blank, formula) match {
      case b: Img[_ <: RealType[_]] => new ImgPlus(b)
      case c: ArrayImg => new ImgPlus(c)
      case _ => throw new IllegalArgumentException("Type not supported")
    }
    println(sinusoid)
     */
    //val ds = ij.dataset().create(sinusoid)

    println(ij.dataset().getDatasets.map(_.toString).mkString(", "))


    val threshCmd = ij.command().getCommand("threshold")
    //println(" ThreshCmd: "+threshCmd.createModule().getInputs.mkString(", "))

  }

  test("Add 5") {
    val addOp = ij.op().op("add", classOf[DoubleType], new DoubleType(5.0))
    //ij.statistics().
  }
}