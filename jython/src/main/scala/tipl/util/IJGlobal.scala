package tipl.util


import fourquant.imagej.scOps.ImageJSettings
import tipl.spark.SparkGlobal

/**
 * For keeping consistent settings for ImageJ, its path and plugin setups
 * Created by mader on 2/5/15.
 */

object IJGlobal {
  var ijPath: String = ""
  var showGui: Boolean = false
  var runLaunch: Boolean = false
  var record: Boolean = false

  lazy val ijsSettings = ImageJSettings(ijPath,showGui,runLaunch,record)
  def getIJS() = ijsSettings
  def activeParser(args: Array[String]): ArgumentParser = {
    val p = SparkGlobal.activeParser(args)
    p.createNewLayer("ImageJ")
    ijPath=p.getOptionPath("@ijpath",ijPath,"Local Path (on each node) to ImageJ/FIJI").
      makeAbsPath().getPath
    showGui=p.getOptionBoolean("@ijshowgui",showGui,"Show the imageJ gui")
    runLaunch=p.getOptionBoolean("@ijlaunch",runLaunch,"Launch ImageJ tools (needed for some " +
      "plugins")
    record=p.getOptionBoolean("@ijrecord",record,"Start recording on ImageJ start")
    p
  }

}
