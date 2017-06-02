import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.Conf
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
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
  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      println("Usage: <dir> <broker> <topic>")
      System.exit(1)
    }
    val imageDir = args(0)
    val kafkaBroker = args(1)
    val topic = args(2)

    val imagePath = "/home/kevin/Downloads/grace_hopper.jpg"
    val imageBytes = Files.readAllBytes(Paths.get(imagePath))


  }
  def publishImage(imageBytes : Array[Byte], description: String): Unit = {
    val record = KafkaProducerRecord("images", Some(description), imageBytes)
    logger.info(s"Sending record to ${record.topic()}")

    val done = producer.send(record)

    Await.result(done, Duration(5, TimeUnit.SECONDS))
    done onComplete {
      case Success(success) => logger.info(s"Committed successfully at offset ${success.offset}")
      case Failure(t) => logger.error("An error has occured: " + t.getMessage)
    }
  }
}
