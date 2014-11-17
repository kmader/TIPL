package tipl.blocks

import java.io.{ByteArrayOutputStream, File, FileWriter, PrintStream}
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.spark.SparkContext
import tipl.formats.TImgRO
import tipl.settings.FilterSettings
import tipl.spark.SparkGlobal
import tipl.util.{ArgumentList, ArgumentParser, TIPLGlobal, TypedPath}

import scala.collection.concurrent.Map
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, future}
import scala.util.{Failure, Success}

object ParameterSweep {
  case class NamedParameter(name: String, parameter: String) {
    def +(nm: NamedParameter): NamedParameter =
      NamedParameter(name+"_"+nm.name,parameter+" "+nm.parameter)

  }


  /**
   * Common sets of parameters to use for Sweeps
   */
  object Parameters {
    val filters = Array(NamedParameter("NearestNeighbor","-filter="+FilterSettings
      .NEAREST_NEIGHBOR),
      NamedParameter("Median","-filter="+FilterSettings.MEDIAN),
      NamedParameter("Gaussian","-filter="+FilterSettings.GAUSSIAN))


    def linearRange(name: String,parameter: String,min: Double, max: Double,
                    steps: Int): Array[NamedParameter] = {
      fixedRange(name,parameter,(0 until steps).map(i => i*(max-min)/steps+min).toArray)
    }

    def logRange(name: String,parameter: String,min: Double, max: Double,
                    steps: Int): Array[NamedParameter] = {
      val lmin = Math.log10(min)
      val lmax = Math.log10(max)
      fixedRange(name,parameter,(0 until steps).map(i => i*(lmax-lmin)/steps+lmin).map(Math.pow
        (10,_)).toArray)
    }

    def fixedRange(name: String, parameter: String, vals: Array[Double]): Array[NamedParameter] =
      vals.map(cval => NamedParameter(name+":"+cval.toString,"-"+parameter+"="+cval.toString))

  }

  object SparkSweep {
    import tipl.util.TIPLOps._
    /**
     * Run sweep over a list of existing parameters
     * @param sc
     * @param inImg image to analyze
     * @param blockName name of the block to run
     * @param parms default starting parameters
     * @param sweepVals the values to sweep with
     * @return
     */
    def runSweep(sc: SparkContext,inImg: TImgRO,blockName: String, parms: String,
                 sweepVals: Array[NamedParameter]*) = {
      var aChain = sc.parallelize(sweepVals.head)
      for (iVals <- sweepVals.tail) {
        val bChain = sc.parallelize(iVals)
        aChain = aChain.cartesian(bChain).map(cval => cval._1 + cval._2)
      }
      val nImg = sc.broadcast(inImg)
      aChain.map{
        curParm =>
          (curParm.name,nImg.value.run(blockName,parms+curParm.parameter))
      }
    }
  }

  val baosMap: Map[String, ByteArrayOutputStream] =
    new scala.collection.concurrent.TrieMap[String, ByteArrayOutputStream]()
  val psMap: Map[String, PrintStream] =
    new scala.collection.concurrent.TrieMap[String, PrintStream]()

  /**
    * A nasty hack to to reroute output streams based on thread names
    */
  class ThreadSeperatedOutputStream extends PrintStream(new ByteArrayOutputStream()) {
    override def println(line: String): Unit = {
      val callerName = Thread.currentThread().getName
      ParameterSweep.baosMap.putIfAbsent(callerName, new ByteArrayOutputStream())
      ParameterSweep.psMap.putIfAbsent(callerName, new PrintStream(baosMap.get(callerName).head))
      psMap.get(callerName).head.println(line)
    }
  }

  /**
    * A class which automatically changes path names to the current analysis being run when writing
    */
  class ThreadSeperatedArgumentParser(customExceptions: Seq[String] = Seq[String]())
    extends TIPLGlobal.ArgumentParserFactory {
    lazy val exceptionList = customExceptions ++ Seq[String]("@localdir", "sge:tiplpath",
      "sge:javapath", "sge:tiplbeta", "sge:sparkpath", "sge:qsubpath", "@sparklocal")

    /**
      * Change the path of the type
      */
    def fixType(argName: String, path: TypedPath, dirName: String) = {
      (argName, path) match {
        case (_, p: TypedPath) if (p.getPath().length < 1 || p.getPath().contains(dirName)) => p
        // if it is empty keep it empty or it is already pathed
        case (arg: String, p: TypedPath) if (arg.toUpperCase().contains("TIPL")) => p
        case (arg: String, p: TypedPath) if (exceptionList.contains(arg)) => p
        case (arg: String, p: TypedPath) =>
          p.getPathType match {
            case TypedPath.PATHTYPE.LOCAL =>
              val pathStr = path.makeAbsPath().getPath.split(File.separator)
              val prefix = pathStr.slice(0, pathStr.length - 1)
              val absDirName = prefix :+ dirName
              val dirFile = new File(absDirName.mkString(File.separator))
              if (dirFile.mkdir) System.out.println(this.getClass().getSimpleName()+":: For "+
                p.getPath()+", Making directory "+dirFile.getPath())

              val prefixedPath = (absDirName :+ pathStr.takeRight(1)(0)).mkString(File.separator)
              path.changePath(prefixedPath)
            case _ => throw new IllegalArgumentException("Thread divided paths are not yet "+
              "supported for:"+path.getPathType)
          }
      }
    }

    def getParser(args: Array[String]): ArgumentParser = {
      new ArgumentParser.CustomArgumentParser(args) {

        override def getOptionPath(argName: String, defaultPath: TypedPath,
          helpText: String): TypedPath = {
          val stdType = super.getOptionPath(argName, defaultPath, helpText)
          val newPath = fixType(argName, stdType, Thread.currentThread().getName())
          val newArg = new ArgumentList.TypedArgument[TypedPath](argName,
            helpText, newPath, ArgumentList.typePathParse);
          putArg(argName, newArg)
          newPath
        }

        override def getOptionPath(argName: String, defaultPath: String,
          helpText: String): TypedPath = {
          val stdType = super.getOptionPath(argName, defaultPath, helpText)
          val newPath = fixType(argName, stdType, Thread.currentThread().getName())
          val newArg = new ArgumentList.TypedArgument[TypedPath](argName,
            helpText, newPath, ArgumentList.typePathParse);
          putArg(argName, newArg)
          newPath
        }
      }
    }
  }

  // begin of parameter sweep specific code

  val prefix = "@ps:"

  object StepType extends Enumeration {
    type StepType = Value
    val LinStep, IntLinStep, LogStep, SqrtStep = Value

    /**
      * Get a string representation of the step (parseInt does not like decimals)
      */
    def getStr(curParm: vargVar, v: Double): String = {
      curParm.stepType match {
        case IntLinStep => "%d".format(v.intValue())
        case _ => v.toString()
      }
    }

    /**
      * Get the nth step using whatever approach has been selected
      */
    def getStep(curParm: vargVar, i: Int): Double = {
      curParm.stepType match {
        case LinStep =>
          if (curParm.varSteps == 1) (curParm.varMax + curParm.varMin) / 2
          else curParm.varMin + (curParm.varMax - curParm.varMin) / (curParm.varSteps - 1) * i
        case IntLinStep =>
          if (curParm.varSteps == 1) Math.round((curParm.varMax + curParm.varMin) / 2)
          else Math.round(curParm.varMin + (curParm.varMax - curParm.varMin) / (curParm.varSteps
            - 1) * i)
        case _ => throw new IllegalArgumentException(curParm.stepType+" has not yet been "+
          "implemented")
      }
    }
  }

  case class vargVar(varName: String, varMin: Double, varMax: Double, varSteps: Int,
    stepType: StepType.StepType)

  def processArgs(p: ArgumentParser): Seq[vargVar] = {
    val vargCount = p.getOptionInt(prefix+"nargs", 1, "The number of arguments to vary", 0,
      Char.MaxValue)
    for (
      curArg <- (1 to vargCount);
      vargName = p.getOptionString(prefix+"argname"+curArg, "",
        "The name of the #"+curArg+" to vary");
      vargMin = p.getOptionDouble(prefix+"argmin"+curArg, Double.MinValue,
        "Minimum value of "+vargName+" (#"+curArg+" argument)");
      vargMax = p.getOptionDouble(prefix+"argmax"+curArg, Double.MaxValue,
        "Maximum value of "+vargName+" (#"+curArg+" argument)");
      vargSteps = p.getOptionInt(prefix+"steps"+curArg, 10, "Number of steps to take "+
        vargName+" (#"+curArg+" argument)", 1, Char.MaxValue);
      vargType = p.getOptionInt(prefix+"type"+curArg, 0, "Number of steps to take "+
        vargName+" (#"+curArg+" argument)", 0, StepType.maxId)
    ) yield vargVar(varName = vargName, varMin = vargMin, varMax = vargMax, varSteps = vargSteps,
      stepType = StepType(vargType))
  }

  def runArguments(methodToCall: Array[String] => Unit, argString: String,
    parameters: Seq[vargVar], isNested: Boolean, appendArgString: String = "")
                  (implicit ec: ExecutionContext): Seq[(String, Future[String])] = {
    val curParm = parameters.head
    var runResults: Seq[(String, Future[String])] = Seq()

    for (i <- 0 until curParm.varSteps) {
      val varVal = StepType.getStep(curParm, i)
      if (i > 0 & (StepType.getStep(curParm, i) == StepType.getStep(curParm, i - 1))) {
        println("Values are identical this step will be skipped")
      }
      else {
        val varStr = StepType.getStr(curParm, varVal)
        val newArgString = "-"+curParm.varName+"="+varStr+" "+appendArgString

        val cleanNAS = (curParm.varName+"="+"%2.2f".format(varVal)+" "+appendArgString)
          .split(" ").mkString("_").split("-").mkString("").split("=").mkString("-")
        val argList = (newArgString+" "+argString).split(" ")
        val execString = methodToCall+" with: "+newArgString+" "+argString

        if (isNested) if (parameters.size > 1) {
          runResults = runResults ++ runArguments(methodToCall, argString, parameters.tail,
            isNested, appendArgString = newArgString) // nested means it is recursive
        }
        else {
          println("Calling:"+execString)
          runResults = runResults :+ (cleanNAS, future {
            val stime = System.currentTimeMillis()
            try {
              Thread.currentThread().setName(cleanNAS)
              System.out.println(this.getClass().getSimpleName()+":: running:"+parameters)
              System.out.println(this.getClass().getSimpleName()+":: Current Step:"+
                newArgString+" in folder:"+cleanNAS)
              methodToCall(argList)
              val eTime = (System.currentTimeMillis() - stime) / (1000F)

              "Suceeded: %2.2f seconds".format(eTime)
            }
            catch {
              case e: Exception => "Failed after %2.2fs:".format((System.currentTimeMillis() -
                stime) / (1000F)) + e.getStackTraceString
            }
          })
        }
      }
    }

    if (!isNested) if (parameters.size > 1) runResults = runResults ++ runArguments(methodToCall,
      argString, parameters.tail, isNested)
    runResults
  }

  def runSweep(methodToCall: Array[String] => Unit,
    argsToProcess: Seq[vargVar],
    newArguments: ArgumentParser,
    inputArguments: Array[String],
    baseLogName: TypedPath,
    waitDuration: Duration, isNested: Boolean, coreCount: Int) = {
    // Start of the execution code

    val originalPS = new PrintStream(System.out)
    val originalEPS = new PrintStream(System.err)
    // Set everything to its own thread
    val tsos = new ThreadSeperatedOutputStream()

    System.setOut(tsos)
    System.setErr(tsos)

    val tsap = new ThreadSeperatedArgumentParser(inputArguments)
    TIPLGlobal.defaultAPFactory = tsap

    // implicit sets as the default context for all future commands
    //TIPLGlobal.requestSimpleES(coreCount))
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(coreCount))

    val output = runArguments(methodToCall, newArguments.toString, argsToProcess, isNested)
    //ec.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)
    val outResults = output.map {
      inval =>
        val (msg: String, results: Future[String]) = inval
        val mainStr = "Exeuction of :"+msg+"\n\t   has\t"
        val outMsg = Await.ready(results, waitDuration).value.get match {
          case Success(result) => result
          case Failure(t) => "Failed:"+t.getMessage()
          case _ => "Something strange happened"
        }

        // write the log files
        baosMap.get(msg).foreach {
          baosLog =>
            if (TIPLGlobal.getDebug()) println(msg+"\t"+baosLog.toString.split("\n").mkString("#"))
            val fixedPath = tsap.fixType("logfile", baseLogName, inval._1)
            println(fixedPath.summary())
            val oPS = new FileWriter(fixedPath.getPath(), false);
            oPS.write(baosLog.toString)
            oPS.write("\n\n\t===== Final Status ====\n"+mainStr +
              outMsg.split("\n").mkString("\t\n"))
            oPS.close()
        }
        mainStr + outMsg.split("\n").mkString("# ")

    }

    // return the outputs to normal

    System.setOut(originalPS)
    System.setErr(originalEPS)
    ec.shutdown()

    outResults
  }

  val className = "ParameterSweep"

  def cmdlineVersion(p: ArgumentParser): Unit = {
    p.createNewLayer("Parameter Sweep Settings")
    val classToCall = p.getOptionString(prefix+"classname", "", "The name of the class or block"+
      " to run")
    val baseLogName = p.getOptionPath(prefix+"logname", className+".log",
      "The name of the log files to keep")
    val isNested = p.getOptionBoolean(prefix+"nested", true, "Are the commands nested (or run "+
      "the loops independently)")
    val coreCount = p.getOptionInt(prefix+"ncores", 2, "The number of commands to run at the "+
      "same time", 1, Char.MaxValue)
    val inputArguments = p.getOptionString(prefix+"inputargs", "", "The name of input arguments"+
      " to avoid directory rerouting (seperated by commas)")
    val waittime = p.getOptionDouble(prefix+"waittime", Int.MaxValue,
      "The number of minutes to wait per job", 0, Int.MaxValue)
    val argsToProcess = processArgs(p)
    val newArguments = p.subArguments(prefix, false)
    // manually add a file to save
    newArguments.getOptionPath(ArgumentParser.saveArg,
      baseLogName.changePath(baseLogName.getPath().split(".log").mkString("", "", ".settings")),
      "Set default settings to the baselog name")
    println(p.getHelp())

    // basically create a class and make sure it has a main method ()
    //NOTE causes problems if class cannot be instantiated!
    //val newPM = Class.forName(classToCall).getMethod("main",Array[String])
    val parmSweepClass = Class.forName(classToCall).newInstance().asInstanceOf[{
      def main(args: Array[String]): Unit
    }]

    val outResults = runSweep(parmSweepClass.main,
      argsToProcess,
      newArguments,
      inputArguments.split(","),
      baseLogName,
      Duration.create(waittime, TimeUnit.MINUTES), isNested, coreCount)

    println("== Results")
    outResults.foreach(println(_))
  }

  def main(args: Array[String]): Unit = {
    val p = SparkGlobal.activeParser(args)
    cmdlineVersion(p)
  }
}
