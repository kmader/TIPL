package tests

import java.io.File
import javax.imageio.ImageIO

import com.github.sarxos.webcam.Webcam
import ij.ImagePlus
import org.scalatest.FunSuite
/**
 * Created by mader on 1/17/15.
 */
class WebcamTests extends FunSuite {
  test("Can webcam be found and read") {
    val webcam = Webcam.getDefault()
    webcam.open()
  }

  test("Can image be saved") {
    val webcam = Webcam.getDefault()
    webcam.open()
    val outFile = new File("/Users/mader/hello-world.png")
    val imgSnap = webcam.getImage()
    assert(imgSnap.getWidth()>0,"Image has non-zero width")
    assert(imgSnap.getHeight()>0,"Image has a non-zero height")
    ImageIO.write(imgSnap, "PNG", outFile )
    assert(outFile.exists(),"Some file was created")
  }

  test("Can multiple images be captured") {
    val webcam = Webcam.getDefault()
    webcam.open()
    var runTime = 0.0
    print("Capture rate:")
      for(i <- 1 to 10) {
        val (imgSnap,ftime) = WebcamTests.time(webcam.getImage())
        runTime+=ftime
        assert(imgSnap.getWidth()>0,"Image has non-zero width")
        assert(imgSnap.getHeight()>0,"Image has a non-zero height")

      }
    println("\n Average Rate: %d".format(Math.round(10/runTime).toInt))
    assert(10/runTime>5,"Can acquire at faster than 5fps")
  }

  test("Can an ImagePlus be created from a webcam image") {
    val webcam = Webcam.getDefault()
    webcam.open()
    val cSnap = webcam.getImage()
    val snapImp = new ImagePlus("testImg", cSnap)
    snapImp.show("Showing")
    Thread.sleep(1000)
    snapImp.close()
  }
}

object WebcamTests {
  def time[A](a: => A) = {
    val now = System.nanoTime
    val result = a
    val secs = (System.nanoTime - now) / 1.0e9
    print("%f fps, ".format(1/secs))
    (result,secs)
  }

}
