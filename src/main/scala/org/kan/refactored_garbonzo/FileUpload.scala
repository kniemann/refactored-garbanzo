package org.kan.refactored_garbonzo

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.StatusCodes
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
      (__ \ "requestTime").format[DateTime]
    ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))

  val routes = {
    pathSingleSlash {
      (post & extractRequest) {
        request => {
          val source = request.entity.dataBytes
          val imageName = request.headers.filter(_.name() == "image_name").head.value
          val done = source
            .map { elem =>
            logger.info(s"Received ${elem.size} bytes with image name $imageName.")
            val imageMetadata = ImageMetadata(imageName, elem.size, "rest_api", DateTime.now)


            val json = Json.toJson(imageMetadata).toString
            logger.info(s"Converted to json $json")
            new ProducerRecord[String, Array[Byte]]("upload_images", json, elem.toArray)
            }
            .runWith(Producer.plainSink(producerSettings))
            .map(_ => s"Finished uploading!")
          onSuccess(done) { done =>
            complete(HttpResponse(status = StatusCodes.OK, entity = done))
          }
        }
      }
    }
  }

  Http().bindAndHandle(routes, config.getString("akka.http.interface"), config.getInt("akka.http.port"))

}
