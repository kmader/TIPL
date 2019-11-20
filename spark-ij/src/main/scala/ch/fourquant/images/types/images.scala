package ch.fourquant.images.types

import fourquant.imagej.PortableImagePlus





/**
 * A very simple construction for storing named sql images
 */
case class NamedSQLImage(path: String, image: PortableImagePlus)


/**
 * A slightly more detailed structure
  *
  * @param path
 * @param name
 * @param parent
 * @param fullpath
 * @param width the width
 * @param height the height
 * @param slices the number of slices (for 3d images)
  * @param image the portableimageplus structure
 */
case class FullSQLImage(path: String, name: String, parent: String,fullpath: Array[String],
                        width: Int, height: Int, slices: Int,
                        image: PortableImagePlus) {
  def this(sample: String, image: PortableImagePlus) =
    this(sample,sample.split("/").reverse.head,
      sample.split("/").reverse.toList.tail.headOption.getOrElse(""),sample.split("/"),
      image.getImg().getWidth,image.getImg().getHeight(),
      image.getImg().getNSlices(),image)

}