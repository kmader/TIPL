import org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK_SER
import org.apache.spark.storage.StorageLevel.DISK_ONLY
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

object filterImage {
    def main(args: Array[String]) {
        
        System.setProperty("spark.executor.memory","40G")
        val sc = new SparkContext(args(0), "FilterTool")
        runFilter(sc,args(1))
    }
    def runFilter(sc: SparkContext,fileName: String) {
        val textImg=sc.textFile(fileName)
        // convert csv to position, value
        val rImg=textImg.map(_.split(",")).map(cLine => ((cLine(0).toInt,cLine(1).toInt,cLine(2).toInt),cLine(3).toDouble))

        // define volume of interest
        def roi(pvec: ((Int,Int,Int),Double)) = {pvec._1._1>=0 & pvec._1._2>=0 & pvec._1._3>=0 &
                                         pvec._1._1<100 & pvec._1._2<100 & pvec._1._3<100}
                                         
        def tinyroi(pvec: ((Int,Int,Int),Double)) = {pvec._1._1>=0 & pvec._1._2>=0 & pvec._1._3>=0 &
                                         pvec._1._1<10 & pvec._1._2<10 & pvec._1._3<10} 
                                         val defpers=MEMORY_AND_DISK_SER
        val roiImg=rImg.filter(roi).persist(defpers)
        // perform a threshold
        val fImg=roiImg.filter(_._2>0)    
        // perform a box filter
        def spread_voxels(pvec: ((Int,Int,Int),Double), windSize: Int = 1) = {
            val wind=(-windSize to windSize)
            val pos=pvec._1
            val scalevalue=pvec._2/(wind.length*wind.length*wind.length)
            for(x<-wind; y<-wind; z<-wind) yield ((pos._1+x,pos._2+y,pos._3+z),scalevalue)
        }

        val filtImg=roiImg.flatMap(cvec => spread_voxels(cvec)).filter(roi).reduceByKey(_ + _)
    }
}