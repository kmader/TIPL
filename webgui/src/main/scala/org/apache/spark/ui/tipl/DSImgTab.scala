package org.apache.spark.ui.tipl

import org.apache.spark.SparkContext
import org.apache.spark.ui.{SparkUI, SparkUITab}
import tipl.spark.DSImg

import scala.collection.mutable.{Map => MuMap}

/**
 * Created by mader on 12/9/14.
 */
class DSImgTab(sc: SparkContext, parent: SparkUI, tabName: String = "Images", rddList:
MuMap[String,DSImg[_]])  extends SparkUITab(parent, tabName) with Serializable {

  def getSavedRDDs = rddList



}

