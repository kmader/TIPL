package org.apache.spark

/**
 * Based on the implementation from the Spark Testing Suite, a set of commands to initialize a
 * Spark context for tests
 * Created by mader on 10/10/14.
 */

import _root_.io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import tipl.spark.SparkGlobal

/** Manages a local `sc` {@link SparkContext} variable, correctly stopping it after each test. */
trait LocalSparkContext extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite =>

  @transient var sc: SparkContext = SparkGlobal.getContext("test",false).sc //_

  override def beforeAll() {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
    super.beforeAll()
  }

  override def afterAll() {
    resetSparkContext()
  }

  override def beforeEach() {
    super.beforeEach()
  }

  override def afterEach() {
    super.afterEach()
  }


  def resetSparkContext() = {
    LocalSparkContext.stop(sc)
    SparkGlobal.stopContext()
    sc = null
  }

  def getSpark(testName: String) =
    SparkGlobal.getContext("Testing:" + testName).sc
  def getSpark(master: String, testName: String ) = {
    SparkGlobal.activeParser(("-@masternode="+master).split("\n"))
    SparkGlobal.getContext("Testing:" + testName).sc
  }
  lazy val masterNode = {
    org.apache.spark.deploy.master.Master.main("".split(","))
    Thread.sleep(5000)
  }

  lazy val workerNode = {
    org.apache.spark.deploy.worker.Worker.main("spark://localhost:8080".split(","))
    Thread.sleep(1000)
  }

}


object LocalSparkContext {

  def stop(sc: SparkContext) {
    SparkGlobal.stopContext()
    // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
    System.clearProperty("spark.driver.port")
  }

  /** Runs `f` by passing in `sc` and ensures that `sc` is stopped. */
  def withSpark[T](sc: SparkContext)(f: SparkContext => T) = {
    try {
      f(sc)
    } finally {
      stop(sc)
    }
  }

}
