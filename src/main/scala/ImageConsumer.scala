import java.util.concurrent.atomic.AtomicLong

import akka.Done

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.{ExecutionContextExecutor, Future}

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

    val done =
      Consumer.committableSource(consumerSettings, Subscriptions.topics("images,upload_images"))
        .mapAsync(1) { msg =>
          logger.info(s"Received Kafka message: ${msg.record.key()} / size of ${msg.record.value.length}")
         val prediction = LabelImage.labelImageFromBytes(msg.record.value())
            .map(guess => (msg.record.key, guess))
         Future{msg}
        }
        .mapAsync(1) { msg =>
          msg.committableOffset.commitScaladsl()
        }
        .runWith(Sink.ignore)
    // #atLeastOnce

    terminateWhenDone(done)
  }
}
