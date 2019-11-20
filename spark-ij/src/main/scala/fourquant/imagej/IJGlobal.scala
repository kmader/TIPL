package fourquant.imagej

import java.awt.GraphicsEnvironment

import fourquant.imagej.ImageJSettings


/**
 * For keeping consistent settings for ImageJ, its path and plugin setups
 * Created by mader on 2/5/15.
 */

object IJGlobal {

  /**
   * These parameters should be adjustable, somehow...
   */
  var ijPath: String = ""
  var showGui: Boolean = false
  var runLaunch: Boolean = false
  var record: Boolean = false

  lazy val ijsSettings = ImageJSettings(ijPath,showGui,runLaunch,record)
  def getIJS() = ijsSettings

  /**
   * Force headless status
   */
  def forceHeadless() {
    try {
      val defaultHeadlessField = classOf[GraphicsEnvironment].getDeclaredField("defaultHeadless")
      defaultHeadlessField.setAccessible(true)
      defaultHeadlessField.set(null, java.lang.Boolean.FALSE)
      val headlessField = classOf[GraphicsEnvironment].getDeclaredField("headless")
      headlessField.setAccessible(true)
      headlessField.set(null, java.lang.Boolean.TRUE)
    }
    catch {
      case e: IllegalAccessException => {
        e.printStackTrace
      }
      case e: NoSuchFieldException => {
        e.printStackTrace
      }
    }
  }

}
