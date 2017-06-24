package org.kan.refactored_garbonzo

import java.util.concurrent.atomic.AtomicLong

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.{Format, JsResultException, Json, __}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import com.github.nscala_time.time.Imports.DateTime
import play.api.libs.functional.syntax.unlift
import play.api.libs.functional.syntax._

/**
  * Created by kevin on 5/19/17.
  */
trait ImageConsumerTrait {
  val logger = Logger(this.getClass)
  val system = ActorSystem("sys")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val m: ActorMaterializer = ActorMaterializer.create(system)

  val maxPartitions = 100

  // #settings
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("images_consumer")
    .withProperty("auto.offset.reset", "earliest")

  class ImageConsumerClass {

    private val offset = new AtomicLong

    def loadOffset(): Future[Long] =
      Future.successful(offset.get)

    def update(data: ConsumerRecord[String, Array[Byte]]): Future[Done] = {
      println(s"Received ${data.key} / ${data.value}")
      Future.successful(Done)
    }
  }

  def terminateWhenDone(result: Future[Done]): Unit = {
    result.onComplete {
      case Failure(e) =>
        system.log.error(e, e.getMessage)
        system.terminate()
      case Success(_) => system.terminate()
    }
  }
}


// Consume messages at-least-once
object ImageConsumer extends ImageConsumerTrait {
  def main(args: Array[String]): Unit = {

    val imageConsumer = new ImageConsumerClass

    implicit val documentFormatter: Format[ImageMetadata] = (
      (__ \ "imageName").format[String] and
        (__ \ "imageSize").format[Int] and
        (__ \ "source").format[String] and
        (__ \ "uuid").format[String] and
        (__ \ "requestTime").format[DateTime]
      ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))

    val done =
      Consumer.committablePartitionedSource(consumerSettings, Subscriptions.topics("images","upload_images"))
        .flatMapMerge(maxPartitions, _._2)
        .mapAsync(1) { msg =>
          //Read metadata object as json
         val imageMetadataOpt: Option[ImageMetadata] = try {
             Some(Json.parse(msg.record.key).as[ImageMetadata])
         } catch {
           case parseException: JsonParseException => {
             logger.error(s"Unable to parse JSON metadata: $parseException")
             None
           }
           case jsException: JsResultException => logger.error("Field missing from JSON:",jsException)
             None
           case e@_ => logger.error("Unknown exception",e)
             None
         }
         imageMetadataOpt match {
            case Some(imageMetadata) => {
              logger.info(s"Received image: ${imageMetadata.imageName} / size of ${msg.record.value.length}")
              val prediction = LabelImage.labelImageFromBytes(msg.record.value())
                .map(guess => (msg.record.key, guess))
              prediction match {
                case Some(result) => logger.info(s"Prediction is ${result}")
                case o@_ => logger.info(s"Unable to find result: $o")
              }
            }
            case None => logger.error("Unable to deserialize image metadata")
         }
         Future{msg}
        }
        .batch(max = 20, first => CommittableOffsetBatch.empty.updated(first.committableOffset)) { (batch, elem) =>
          batch.updated(elem.committableOffset)
        }
        .mapAsync(3)(_.commitScaladsl())
        .runWith(Sink.ignore)
    terminateWhenDone(done)
  }
}
