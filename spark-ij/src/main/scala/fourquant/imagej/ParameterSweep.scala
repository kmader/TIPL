package fourquant.imagej

import java.io.File

import scala.collection.Map
import scala.collection.mutable.{Map => MMap}


object ParameterSweep {
  case class NamedParameter(name: String, parameter: String) {
    def +(nm: NamedParameter): NamedParameter =
      NamedParameter(name+"_"+nm.name,parameter+" "+nm.parameter)

  }


  /**
   * Common sets of parameters to use for Sweeps
   */
  object Parameters {

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



  /**
   * Created by mader on 3/3/15.
   */
  object ImageJSweep extends Serializable {
    import scala.util.control.Exception.allCatch
    private def isLongNumber(s: String): Boolean = (allCatch opt s.toLong).isDefined
    private def isDoubleNumber(s: String): Boolean = (allCatch opt s.toDouble).isDefined

    def parseArgsWithDelim(argList: String, sptChar: String  = "-") = {
      argList.replaceAll("\\s+", " ").trim().split(sptChar).map(_.trim).filter(_.length > 0).map {
        inArg =>
          val argP = inArg.split("=").zipWithIndex.map(_.swap).toMap
          (argP.getOrElse(0, "ERROR"), argP.getOrElse(1, "true"))
      }.toMap
    }

    /**
     * just a useful class for pulling argument values from a map
     * @param am
     */
    implicit class argMap(am: Map[String,String]) {
      def getDbl(key: String): Option[Double] = am.get(key) match {
        case Some(dblVal) if isDoubleNumber(dblVal) => Some(dblVal.toDouble)
        case _ => None
      }
      def getDbl(key: String, defVal: Double): Double = getDbl(key) match {
        case Some(dblVal) => dblVal
        case _ => defVal
      }
      def getInt(key: String): Option[Int] = am.get(key) match {
        case Some(intVal) if isLongNumber(intVal) => Some(intVal.toInt)
        case _ => None
      }
      def getInt(key: String, defVal: Int): Int = getInt(key) match {
        case Some(intVal) => intVal
        case _ => defVal
      }
    }

    /**
     *
     * @param inSteps the steps of macro arguments
     * @param steps how many steps between start and end
     * @param cartesian cross argument list (steps**arguments instead of steps)
     * @param distinct keep only distinct elements in the arguments (allows
     *                                  interpolation for 2+ steps if there are only 2 unique values
     * @return
     */
    def ImageJMacroStepsToSweep(inSteps: Array[String],
                                steps: Int = 5,
                                cartesian: Boolean = true,
                                distinct: Boolean = true,
                                delim: String = "-"): Array[String] = {

      val pa = macroParseArgs(inSteps,delim)
      val prefix = if(delim.equalsIgnoreCase("-")) "-" else ""
      sweepArgs(pa,prefix,steps,cartesian,distinct)
    }

    def SweepToPath(sweepSteps: Array[String],
                    newDirectories: Boolean = true,
                    removeStatic: Boolean = true,
                    delim: String): Array[String] = {
      val sweepMapParsed = sweepSteps.map(parseArgsWithDelim(_,delim))
      val sweepMap = sweepMapParsed.zipWithIndex.
        foldLeft(MMap.empty[String,Array[String]]) {
        case (fullMap, (newMap,newInd)) =>
          newMap.foreach {
            case (cKey, cVal) =>
              val oMap = fullMap.getOrElse(cKey,Array.fill(sweepSteps.length)("false"))
              oMap(newInd)=cVal
              fullMap.put(cKey,oMap)
          }
          fullMap
      }
      println(sweepMap)

      val filteredMap = if (removeStatic) {
        val keepKeys = sweepMap.filterNot{
          case(key,kmap) =>
            val firstValue = kmap.head
            kmap.forall(_.equalsIgnoreCase(firstValue))
        }.keySet
        println(keepKeys)
        sweepMap.filterKeys(keepKeys.contains(_))
      } else {
        sweepMap
      }
      val valSep = "_"
      val argSep = if(newDirectories) {
        File.separator
      } else {
        "__"
      }
      filteredMap.map{
        case (key,vallist) => vallist.map(key + valSep + _).toList
      }.transpose.
        map(_.reduce(_ + argSep + _)).toArray
    }

    /**
     * A parameter sweep based on 2 sets of arguments
     * @param startArgs the starting set of macro arguments
     * @param endArgs the final set of macro arguements
     *                @note if the value of an argument is an integer 1 (vs 1.0) it will be
     *                      treated as if this value MUST be an integer)
     * @return a parse list of arguments
     */
    protected[ParameterSweep] def macroParseArgs(startArgs: String, endArgs: String,
                                         delim: String): Map[String, Array[String]] =
      macroParseArgs(Array(startArgs, endArgs),delim)

    protected[ParameterSweep] def macroParseArgs(inArgs: Array[String],
                                         delim: String
                                          ): MMap[String, Array[String]] = {

      val argMap = inArgs.map(parseArgsWithDelim(_,delim))

      val allKeys = argMap.map(_.keySet).reduce(_ ++ _)
      val joinArgs = MMap[String, Array[String]]()

      argMap.zipWithIndex.foreach {
        case (cMap,cInd) =>
          cMap.foreach {
            cKey =>
              val oldVal = joinArgs.getOrElse(cKey._1, Array.fill(argMap.length)("false"))
              oldVal(cInd)=cKey._2
              joinArgs(cKey._1)=oldVal
          }
      }
      joinArgs
    }

    private def createArgStr(key: String, arg: String,prefix: String) = {
      arg match {
        case "true" => prefix+key
        case "false" => ""
        case _ => prefix+key+"="+arg
      }
    }

    /**
     *
     * @param parseArgs the parse arguments from macroParseArgs
     * @param steps how many steps between start and end
     * @param cartesian cross argument list (steps**arguments instead of steps)
     *                  @param distinct keep only distinct elements in the arguments (allows
     *                                  interpolation for 2+ steps if there are only 2 unique values
     * @return a list of macro arguments to use
     */
    protected[ParameterSweep] def sweepArgs(parseArgs: Map[String,Array[String]],
                                    prefix: String, steps: Int = 5,
                                    cartesian: Boolean=true, distinct: Boolean=true):
    Array[String] = {
      val varArgs = parseArgs.map{
        case(key,range) =>
          (key,
            if (range.forall(_.equalsIgnoreCase(range.head))) {
              Array(range.head)
            } else {
              if(distinct) {
                range.distinct
              } else {
                range
              }
            })
      }

      val stepList = (0 until steps).map(_/(steps-1.0))
      val arrArgs = varArgs.map{
        case(key,range) =>
          (key,
            range.head match {
              case intStr if isLongNumber(intStr) & (range.length==2) =>
                // if it is a long, treat it as if it has to be an integer
                val intRng = (range.head.toFloat,range.last.toFloat)
                stepList.map(_*(intRng._2-intRng._1) + intRng._1).
                  map(_.toInt).distinct.map(_.toString).toArray
              case floatStr if isDoubleNumber(floatStr) & (range.length==2) =>
                val fltRng = (range.head.toFloat,range.last.toFloat)
                stepList.map(_*(fltRng._2-fltRng._1) + fltRng._1).
                  map(_.toString).toArray
              case _ =>
                range
            }
            )
      }
      val exArrArgs = if(cartesian || steps==2) {
        arrArgs
      } else {
        // if it is not cartesian all of the two length arguments must be interpolated to length
        // steps
        arrArgs.map{
          case(key,rangeVals) =>
            (key,
              rangeVals match {
                case inArr if (inArr.length==steps) =>
                  inArr
                case inArr if (inArr.length==2) =>
                  val bList = (Array.fill(steps/2)(inArr(0)) ++ Array.fill(steps/2)(inArr(1)))
                  (steps % 2) match {
                    case 0 =>
                      bList.toArray
                    case 1 =>
                      (bList ++ Array(inArr(1))).toArray
                  }
                case inArr =>
                  println("The array is neither length 2 nor steps, will be padded")
                  val nArr = Array.fill((steps-inArr.length)/2)(inArr.head) ++ inArr
                  nArr ++ Array.fill((steps-nArr.length))(inArr.last)

              })
        }
      }

      val argStr = exArrArgs.map{
        case(key,rangeVals) =>
          rangeVals.map(arg => createArgStr(key,arg,prefix))
      }

      val allVarArgs = if(cartesian) {
        argStr.foldLeft(Seq.empty[String]){
          (inx,iny) =>
            (inx,iny) match {
              case (x,y) if (x.size>0) & (y.size>0) => for(ax <- x; ay <- y) yield ax+" "+ay
              case (x,y) if (x.size>0) => for(ax <- x) yield ax
              case (x,y) if (y.size>0) => for(ay <- y) yield ay
              case (x,y) => x
            }
        }.toArray
      } else {
        argStr.reduce{
          (aArgs, bArgs) =>
            aArgs.zip(bArgs).map{ case(aStr,bStr) => aStr+" "+bStr}
        }.toArray
      }

      allVarArgs.
        map(_.replaceAll("\\s+", " ").trim()).toArray
    }
  }



}
