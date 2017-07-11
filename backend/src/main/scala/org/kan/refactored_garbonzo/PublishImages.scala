package org.kan.refactored_garbonzo

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.github.nscala_time.time.Imports.DateTime
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import play.api.libs.functional.syntax.unlift
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import play.api.libs.functional.syntax._

import scala.annotation.tailrec

/**
  * Created by kevin on 5/19/17.
  */
class PublishImages {

}
object PublishImages {
  val logger = Logger(this.getClass)
  val producer = KafkaProducer(
    Conf(new StringSerializer(), new ByteArraySerializer(), bootstrapServers = "localhost:9092")
  )
  implicit val documentFormatter: Format[ImageMetadata] = (
    (__ \ "imageName").format[String] and
      (__ \ "imageSize").format[Int] and
      (__ \ "source").format[String] and
      (__ \ "uuid").format[String] and
      (__ \ "requestTime").format[DateTime]
    ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: <image path> <description>")
      System.exit(1)
    }
//    val imageDir = args(0)
//    val kafkaBroker = args(1)
//    val topic = args(2)

    val imagePath = args(0)
    val description = args(1)

    //val imagePath = "/home/kevin/Downloads/grace_hopper.jpg"
//    val imagePath = "/home/kevin/git/tensorflow_scala/src/main/resources/flower_photos/daisy/5547758_eea9edfd54_n.jpg"
    val pattern = """(.*)\*$""".r

    imagePath match {
      case pattern(dir, _*) => {
        logger.info(s"Scanning $dir")
        val path = new File(dir)
        val listFiles = listAllFiles(path)
        val listJpg = listFiles.filter(f => isJpg(f.toString) || isJpeg(f.toString))
        listJpg.foreach( file => {
          val imageBytes = Files.readAllBytes(file.toPath)
          publishImage(imageBytes,file.getName)
        })
      }
      case _ => {
        val imageBytes = Files.readAllBytes(Paths.get(imagePath))
        publishImage(imageBytes,description)
      }
    }
  }
  def publishImage(imageBytes : Array[Byte], description: String): Unit = {
    val imageUUID = java.util.UUID.randomUUID.toString
    val imageMetadata = ImageMetadata(
      description,
      imageBytes.length,
      "file_api",
      imageUUID,
      DateTime.now)
    val json = Json.toJson(imageMetadata).toString
    logger.info(s"Converted metadata to json $json with payload containing ${imageBytes.length} bytes.")

    val record = KafkaProducerRecord("images", json, imageBytes)
    logger.info(s"Sending record to ${record.topic()}")

    val done = producer.send(record)



    Await.result(done, Duration(5, TimeUnit.SECONDS))
    done onComplete {
      case Success(success) => logger.info(s"Committed successfully at offset ${success.offset}")
      case Failure(t) => logger.error("An error has occured: " + t.getMessage)
    }
  }
  def listAllFiles(file: File): Array[File] = {
    val list = file.listFiles
    list ++ list.filter(_.isDirectory).flatMap(listAllFiles)
  }
  def isJpg(fileName: String) : Boolean = fileName.toLowerCase.endsWith(".jpg")
  def isJpeg(fileName: String) : Boolean = fileName.toLowerCase.endsWith(".jpeg")
}
