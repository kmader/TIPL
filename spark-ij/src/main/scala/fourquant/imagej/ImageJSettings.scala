package fourquant.imagej

import ij.IJ
import org.apache.spark.SparkContext
import org.apache.spark.sql.UDFRegistration

/**
  * A class which hangs around and keeps all of the imagej settings (so they can be sent to
  * workers)
  *
  * @param showGui
  * @param ijPath location of fiji path
  *
  */
case class ImageJSettings(ijPath: String,
                          showGui: Boolean = false,
                          runLaunch: Boolean = true,
                          record: Boolean = false,
                          forceHeadless: Boolean = false,
                          tryInstantiateIJ: Boolean = true
                         ) extends FijiInit {
  def instantiateIJ = !forceHeadless & tryInstantiateIJ

  /**
    * The initial number of partitions to create
    *
    * @return default is 30, but can be overridden for other cluster configurations
    */
  def initPartitionCount() = 30

  /**
    * The number of elements in the first grouping, the elements are divided over initPartitionCount partitions
    *
    * @return per default 100 but can be lower for many cases
    */
  def initElementCount() = 100

  /**
    * If there are any special or additional functions to be registered by the current IJ settings
 *
    * @param sq_udf the context to register the functions to
    */
  def registerSQLFunctions(sq_udf: UDFRegistration): Unit = {}
  /**
    * If there are any special or additional functions to be registered by the current IJ settings
 *
    * @param sq_udf the context to register the functions to
    */
  def registerSQLDebugFunctions(sq_udf: UDFRegistration): Unit = {}

  def setupSpark(sc: SparkContext, partRun: Boolean = false) = {
    if(!showGui) {
      if (forceHeadless) IJGlobal.forceHeadless()
      sc.setLocalProperty("java.awt.headless","false")
    }
    sc.parallelize(1 to initPartitionCount,initElementCount).
      mapPartitions{
        ip =>
          setupFiji()
          ip
      }.collect()
  }

  /**
    * Get the current instance of imagej (if available)
    */
  def getInstance() = {
    if (instantiateIJ) {
      IJ.URL.synchronized {
        if (IJ.getInstance()==null) {
          new ij.ImageJ(ij.ImageJ.NO_SHOW);
          if (IJ.getInstance()==null)
            throw new IllegalArgumentException("No Instance of ImageJ Found and could not be started")
        }
        Some(IJ.getInstance())
      }
    } else {
      None
    }
  }

  override def setupFiji() = {
    if(!showGui) {
      if (forceHeadless) IJGlobal.forceHeadless();
      System.setProperty("java.awt.headless","false")
    }
    IJ.URL.synchronized {
      scOps.StartFiji(ijPath, showGui, runLaunch, record)
    }

    getInstance()


  }
}