/**
 * Based on the implementation from the Spark Testing Suite, a set of commands to initialize a
 * Spark context for tests
 * Created by mader on 10/10/14.
 */

package tipl.spark

import _root_.io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import org.apache.spark.SparkContext
import tipl.spark.SparkGlobal


/** Manages a local `sc` {@link SparkContext} variable, correctly stopping it after each test. */
trait LocalSparkContext extends BeforeAndAfterEach with BeforeAndAfterAll {
  self: Suite =>

  @transient var sc: SparkContext = _

  override def beforeAll() {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
    super.beforeAll()
  }

  override def afterEach() {
    resetSparkContext()
    super.afterEach()
  }

  def resetSparkContext() = {
    LocalSparkContext.stop(sc)
    sc = null
  }

  def getSpark(testName: String) = SparkGlobal.getContext("Testing:" + testName).sc

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
