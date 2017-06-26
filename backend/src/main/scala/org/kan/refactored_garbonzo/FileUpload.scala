package org.kan.refactored_garbonzo

import java.io.File
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import com.github.nscala_time.time.Imports.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.stream.scaladsl._
import akka.http.scaladsl.model.Multipart

import scala.concurrent.duration._
import scala.concurrent.Future

object FileUpload extends App {

  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers("localhost:9092")

  implicit val documentFormatter: Format[ImageMetadata] = (
    (__ \ "imageName").format[String] and
      (__ \ "imageSize").format[Int] and
      (__ \ "source").format[String] and
      (__ \ "uuid").format[String] and
      (__ \ "requestTime").format[DateTime]
    ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))


  val uploadImages =
      path("images") {
        entity(as[Multipart.FormData]) { formData =>
          val imageUUID = java.util.UUID.randomUUID.toString
          // collect all parts of the multipart as it arrives into a map
          val allPartsF: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {

            case b: BodyPart if b.name == "file" =>
              // stream into a file as the chunks of it arrives and return a future
              // file to where it got stored
              val file = File.createTempFile("upload", "tmp")
              b.entity.dataBytes.runWith(FileIO.toPath(file.toPath))(materializer).map(_ =>
                (b.name -> file))

            case b: BodyPart =>
              // collect form field values
              b.toStrict(Duration(2,SECONDS)).map(strict =>
                (b.name -> strict.entity.data.utf8String))

          }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

          val done = Source.fromFuture(allPartsF).map { allParts =>
            val file = allParts("file").asInstanceOf[File]
            val imageBytes = Files.readAllBytes(file.toPath)
            val imageName = allParts("name").asInstanceOf[String]
            logger.info(s"Received ${imageBytes.length} bytes with image name $imageName.")
            val imageMetadata = ImageMetadata(
              imageName,
              imageBytes.length,
              "rest_api",
              imageUUID,
              DateTime.now)
            val json = Json.toJson(imageMetadata).toString
            logger.info(s"Converted to json $json")
            new ProducerRecord[String, Array[Byte]]("upload_images", json, imageBytes)
          }
            .runWith(Producer.plainSink(producerSettings))
            .map(_ => s"Finished uploading!")

          // when processing have finished create a response for the user
          onSuccess(allPartsF) { allParts =>
            complete {
              imageUUID
            }
          }
        }
      }
  Http().bindAndHandle(uploadImages, config.getString("akka.http.interface"), config.getInt("akka.http.port"))

}
