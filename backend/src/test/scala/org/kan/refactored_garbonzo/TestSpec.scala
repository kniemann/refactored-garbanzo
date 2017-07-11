package org.kan.refactored_garbonzo

import java.io.IOException
import java.nio.file.{Files, Path, Paths}

import org.scalatest._

class TestSpec extends FlatSpec with Matchers {

  "LabelImage" should "label an image" in {
    val imageFile = "backend/src/test/resources/tulip.jpg"
    val imageBytes = readAllBytesOrExit(Paths.get(imageFile))
    val image = LabelImage.labelImageFromBytes(imageBytes)
    println(image.get._1,image.get._2)
    val expected = 43.682114f
    assert(image.get._1 === "hip")
    assert(image.get._2 === expected)


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

}
