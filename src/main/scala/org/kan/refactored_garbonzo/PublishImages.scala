package org.kan.refactored_garbonzo

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
      (__ \ "requestTime").format[DateTime]
    ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))

  def main(args: Array[String]): Unit = {
//    if (args.length != 3) {
//      println("Usage: <dir> <broker> <topic>")
//      System.exit(1)
//    }
//    val imageDir = args(0)
//    val kafkaBroker = args(1)
//    val topic = args(2)

    //val imagePath = "/home/kevin/Downloads/grace_hopper.jpg"
    val imagePath = "/home/kevin/git/tensorflow_scala/src/main/resources/flower_photos/daisy/5547758_eea9edfd54_n.jpg"
    val imageBytes = Files.readAllBytes(Paths.get(imagePath))
    publishImage(imageBytes,"Grace Hopper")


  }
  def publishImage(imageBytes : Array[Byte], description: String): Unit = {
    val imageMetadata = ImageMetadata(description, imageBytes.length, "file_api", DateTime.now)
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
}
