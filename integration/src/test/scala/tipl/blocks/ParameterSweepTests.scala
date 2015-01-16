package tipl.blocks

import org.scalatest.FunSuite
import tipl.blocks.ParameterSweep.ImageJSweep

/**
 * Created by mader on 1/15/15.
 */
class ParameterSweepTests extends FunSuite  {
  var testSet = "ImageJ Parsing"
  test(testSet+" Simple Sweep") {
    val stArgs = "-radius=3"
    val enArgs = "-radius=6"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    println(swArgs)
    assert(swArgs.contains("radius"))
    assert(swArgs("radius").head.equalsIgnoreCase("3"))
    assert(swArgs("radius").last.equalsIgnoreCase("6"))
  }
  test(testSet+" Missing Sweep") {
    val stArgs = "-filter"
    val enArgs = "-skip"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    println(swArgs)
    assert(swArgs.contains("filter"))
    assert(swArgs("filter").head.equalsIgnoreCase("true"))
    assert(swArgs("filter").last.equalsIgnoreCase("false"))
    assert(swArgs.contains("skip"))
    assert(swArgs("skip").head.equalsIgnoreCase("false"))
    assert(swArgs("skip").last.equalsIgnoreCase("true"))
  }
  testSet = "ImageJ Sweeping"
  test(testSet+" Simple Sweep") {
    val stArgs = "-radius=3.0 -filter"
    val enArgs = "-radius=6.0 -filter"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    val swVals = ImageJSweep.sweepArgs(swArgs,"-",5,false)

    println(swVals.mkString("\n"))
    assert(swVals.length==5)
    assert(swVals.head.contains("-radius=3.0"))
    assert(swVals.last.contains("-radius=6.0"))
    assert(swVals.forall(_.contains("-filter")))
  }

  test(testSet+" Integer Sweep") {
    val stArgs = "-radius=3"
    val enArgs = "-radius=6"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    val swVals = ImageJSweep.sweepArgs(swArgs,"-",5,false)
    println(swVals.mkString("\n"))
    assert(swVals.length==5)
    assert(swVals.head.equalsIgnoreCase("-radius=3"))
    assert(swVals.last.equalsIgnoreCase("-radius=6"))
  }

  test(testSet+" Simple Cartesian") {
    val stArgs = "-radius=3.0"
    val enArgs = "-radius=6.0"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    val swVals = ImageJSweep.sweepArgs(swArgs,"-",5,true)

    println(swVals.mkString("\n"))
    assert(swVals.length==5)
  }
  test(testSet+" Fancy Sweep") {
    val stArgs = "-radius=3.0 -filter"
    val enArgs = "-radius=6.0 -skip"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    val swVals = ImageJSweep.sweepArgs(swArgs,"-",5,false)

    println(swVals.mkString("\n"))
    assert(swVals.length==5)
    assert(swVals.head.contains("-radius=3.0"))
    assert(swVals.head.contains("-filter"))
    assert(!swVals.head.contains("-skip"))
    assert(swVals.last.contains("-radius=6.0"))
    assert(swVals.last.contains("-skip"))
    assert(!swVals.last.contains("-filter"))
  }

  test(testSet+" Fancy Cartesian Sweep") {
    val stArgs = "-radius=3.0 -filter"
    val enArgs = "-radius=6.0"
    val swArgs = ImageJSweep.macroParseArgs(stArgs,enArgs,"-")
    val swVals = ImageJSweep.sweepArgs(swArgs,"-",3,true)

    println(swVals.mkString("\n"))
    assert(swVals.length==6)
  }

  test(testSet+" Fancy Cartesian Sweep No Dash") {
    val stArgs = "radius=3.0 filter"
    val enArgs = "radius=6.0"
    val swVals = ImageJSweep.ImageJMacroStepsToSweep(Array(stArgs,enArgs),steps=3,
      cartesian=true,delim=" ")

    println(swVals.mkString("\n"))
    assert(swVals.length==6)
    assert(swVals.head.contains("radius=3.0"))
    assert(swVals.last.contains("radius=6.0"))
    assert(swVals.head.contains("filter"))
    assert(!swVals.last.contains("filter"))

  }

  testSet = "ImageJ Macro Sweeping"
  test(testSet+" Fancy Sweep") {
    val stArgs = "-radius=3.0 -filter"
    val enArgs = "-radius=6.0 -skip"
    val swVals = ImageJSweep.ImageJMacroStepsToSweep(Array(stArgs,enArgs),5,false)

    println(swVals.mkString("\n"))
    assert(swVals.length==5)
    assert(swVals.head.contains("-radius=3.0"))
    assert(swVals.head.contains("-filter"))
    assert(!swVals.head.contains("-skip"))
    assert(swVals.last.contains("-radius=6.0"))
    assert(swVals.last.contains("-skip"))
    assert(!swVals.last.contains("-filter"))
  }

  test(testSet+" Complex Analysis") {
    val args = Array(
      "-radius=3.0 -filter=gaussian -3dimages",
      "-radius=6.0 -filter=median",
      "-radius=6.0 -filter=nofilter"
    )
    val swVals = ImageJSweep.ImageJMacroStepsToSweep(args,5,true)

    assert(swVals.length==30)
    assert(swVals.head.contains("-radius=3.0"))
    assert(swVals.head.contains("-filter=gaussian"))
    assert(swVals.head.contains("-3dimages"))
    assert(swVals.last.contains("-radius=6.0"))
    assert(swVals.last.contains("-filter=nofilter"))
    assert(!swVals.last.contains("-3dimages"))
  }

testSet = "ImageJ Correct Path Names"
  test(testSet+" Simple Sweep") {
    val args = Array("-radius=3.0 -filter" ,
      "-radius=6.0 -filter -skip")
    val swArgs = ImageJSweep.ImageJMacroStepsToSweep(args,5,true)
    val swPathsDirs = ImageJSweep.SweepToPath(swArgs,true,delim="-")
    val swPathsFiles = ImageJSweep.SweepToPath(swArgs,false,delim="-")
   // println(swPathsFiles.zip(swArgs).map(a=>a._1 + "\tArgs[" + a._2+"]").mkString("\n"))
    assert(swPathsFiles.head.contains("skip_false"))
    assert(swPathsFiles.last.contains("skip_true"))

    assert(swPathsDirs.head.contains("skip_false"))
    assert(swPathsDirs.last.contains("skip_true"))

    assert(!swPathsFiles.head.contains(java.io.File.separator) )
      assert(!swPathsDirs.head.contains("__"))


  }
}

