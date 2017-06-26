package org.kan.refactored_garbonzo

import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}
import java.util
import com.typesafe.scalalogging.Logger
import org.tensorflow._

case class LabelImageRequest(description: String, imageBytes: Array[Byte])

object LabelImage {
  val logger = Logger(this.getClass)

  def labelImageFromBytes(imageBytes: Array[Byte]): Option[(String, Float)] = {
      val modelDir = "/home/kevin/Git/javasandbox/tensorflow/src/main/resources/inception"
      val graphDef = readAllBytesOrExit(Paths.get(modelDir, "tensorflow_inception_graph.pb"))
      val labels = readAllLinesOrExit(Paths.get(modelDir, "imagenet_comp_graph_label_strings.txt"))
      val md = java.security.MessageDigest.getInstance("SHA-1")
      val ha = new sun.misc.BASE64Encoder().encode(md.digest(imageBytes))

    logger.info(s"Labeling image with length ${imageBytes.length} hash $ha")
      val normalizedImage = constructAndExecuteGraphToNormalizeImage(imageBytes)
      normalizedImage match {
        case Some(image) =>
          try {
            val labelProbabilities = executeInceptionGraph(graphDef, image)
            val result = labelProbabilities match {
              case Some(probable) =>
                logger.info("Completed image recognition process, determining label.")
                val bestLabelIdx = maxIndex(probable)
                val label = labels.get(bestLabelIdx)
                val probability = probable(bestLabelIdx) * 100f
                logger.info(s"BEST MATCH $label ($probability% likely)")
                Some(label, probability)
              case None => logger.error("Unable to execute graph.")
                None
            }
            logger.info(s"Returning ${result}")
            result
          } finally if (image != null) image.close()
        case None =>
          logger.error("No image found.")
          None
      }
  }

  private def readAllBytesOrExit(path: Path): Array[Byte] = {
    try
      return Files.readAllBytes(path)
    catch {
      case e: IOException =>
        System.err.println("Failed to read [" + path + "]: " + e.getMessage)
        System.exit(1)
    }
    null
  }

  private def readAllLinesOrExit(path: Path): util.List[String] = {
    try
      return Files.readAllLines(path, Charset.forName("UTF-8"))
    catch {
      case e: IOException =>
        System.err.println("Failed to read [" + path + "]: " + e.getMessage)
        System.exit(0)
    }
    null
  }

  // In the fullness of time, equivalents of the methods of this class should be auto-generated from
  // the OpDefs linked into libtensorflow_jni.so. That would match what is done in other languages
  // like Python, C++ and Go.
  class GraphBuilder(var g: Graph) {
    def div(x: Output, y: Output): Output = binaryOp("Div", x, y)

    def sub(x: Output, y: Output): Output = binaryOp("Sub", x, y)

    def resizeBilinear(images: Output, size: Output): Output = binaryOp("ResizeBilinear", images, size)

    def expandDims(input: Output, dim: Output): Output = binaryOp("ExpandDims", input, dim)

    def cast(value: Output, dtype: DataType): Output = g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build.output(0)

    def decodeJpeg(contents: Output, channels: Long): Output = g.opBuilder("DecodeJpeg", "DecodeJpeg").addInput(contents).setAttr("channels", channels).build.output(0)

    def constant(name: String, value: Any): Output = try {
      val t = Tensor.create(value)
      try
        g.opBuilder("Const", name).setAttr("dtype", t.dataType).setAttr("value", t).build.output(0)
      finally if (t != null) t.close
    }

    private def binaryOp(`type`: String, in1: Output, in2: Output) = g.opBuilder(`type`, `type`).addInput(in1).addInput(in2).build.output(0)
  }

  private def maxIndex(probabilities: Array[Float]) : Int = {
    var best = 0
    var i = 1
    while ( {
      i < probabilities.length
    }) {
      if (probabilities(i) > probabilities(best)) best = i

      {
        i += 1; i
      }
    }
    best
  }

  private def constructAndExecuteGraphToNormalizeImage(imageBytes: Array[Byte]) : Option[Tensor] = try {
    val graph = new Graph
    try {
      val builder = new LabelImage.GraphBuilder(graph)
      // Some constants specific to the pre-trained model at:
      // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
      //
      // - The model was trained with images scaled to 224x224 pixels.
      // - The colors, represented as R, G, B in 1-byte each were converted to
      //   float using (value - Mean)/Scale.
      val H = 224
      val W = 224
      val mean = 117f
      val scale = 1f
      // Since the graph is being constructed once per execution here, we can use a constant for the
      // input image. If the graph were to be re-used for multiple input images, a placeholder would
      // have been more appropriate.
      val input = builder.constant("input", imageBytes)
      val output = builder.div(builder
        .sub(builder
          .resizeBilinear(builder
            .expandDims(builder
              .cast(builder
                .decodeJpeg(input, 3), DataType.FLOAT), builder
              .constant("make_batch", 0)), builder
            .constant("size", Array[Int](H, W))), builder
          .constant("mean", mean)), builder
        .constant("scale", scale))
      try {
        val session = new Session(graph)
        try {
          Some(session.runner.fetch(output.op.name).run.get(0))
        } catch {
          case exception: IllegalArgumentException =>
            logger.error(s"Got this unknown running session: $exception")
            None
        }
        finally if (session != null) session.close()
      } catch {
        case exception: IllegalArgumentException =>
          logger.error(s"Got this unknown exception creating session $exception")
          None
      }
    } finally if (graph != null) graph.close()
  }

  private def executeInceptionGraph(graphDef: Array[Byte], image: Tensor) : Option[Array[Float]] = {
    logger.info("Executing inception graph.")
    val graph = new Graph
    val session = new Session(graph)
    try {
      graph.importGraphDef(graphDef)
      val result = session.runner.feed("input", image).fetch("output").run.get(0)
      val rshape = result.shape
      if (result.numDimensions != 2 || rshape(0) != 1) throw new RuntimeException(String.format("Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s", util.Arrays.toString(rshape)))
      val nlabels = rshape(1).toInt
      val rankedPredictions = Array.ofDim[Float](1, nlabels)
      logger.info("Returning inception graph.")
      Some(result.copyTo(rankedPredictions)(0))
    } catch {
      case o@_ => logger.error(s"Exception building graph $o",o)
        throw o
    } finally {
      if (session != null) session.close()
      if (graph != null) graph.close()
    }
  }


//  override def receive: Receive = {
//    case LabelImageRequest(description, imageBytes) => logger.info("received test")
//    case _      => logger.info("received unknown message")
//  }
}
