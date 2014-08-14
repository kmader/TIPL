/**
 *
 */
/**
 * The spark package is a collection of tools for integrating a TIPL analysis workflow into a Spark cluster. The primary tool inside the spark package is the DTImg structure which wraps around the standard RDD in order to make a TImg-like object. Eventually the code will be adapted to DTImg and TImgs can be used interchangeable in all plugins and will (if available) automatically be distributed across a spark cluster.
 * @author mader
 *
 */
package tipl.spark;