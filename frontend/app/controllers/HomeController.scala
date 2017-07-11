package controllers

import java.io.File
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import java.util
import java.util.Collections
import javax.inject._

import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.{ExecutionContext, Future}
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.Conf
import com.datastax.driver.core.{Cluster, Row}
import com.github.nscala_time.time.Imports.DateTime
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{Format, __}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.JavaConverters
case class FormData(name: String)

/**
 * This controller handles a file upload.
 */
@Singleton
class HomeController @Inject() (implicit val messagesApi: MessagesApi, ec: ExecutionContext) extends Controller with i18n.I18nSupport {

  case class ImageMetadata(imageName: String,
                           imageSize: Int,
                           source: String,
                           uuid: String,
                           requestTime: DateTime)
  implicit val documentFormatter: Format[ImageMetadata] = (
    (__ \ "imageName").format[String] and
      (__ \ "imageSize").format[Int] and
      (__ \ "source").format[String] and
      (__ \ "uuid").format[String] and
      (__ \ "requestTime").format[DateTime]
    ) (ImageMetadata.apply, unlift(ImageMetadata.unapply))

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new ByteArraySerializer(), bootstrapServers = "localhost:9092")
  )

  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  val form = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
  )

  /**
   * Renders a start page.
   */
  def index = Action { implicit request =>
    Ok(views.html.index(form))
  }

  /**
    * Renders a table.
    */
  def table = Action { implicit request =>
    val rs = CassandraClient.getValueFromCassandraTable
    val buffer = JavaConverters.asScalaBufferConverter(rs.all).asScala
    val sortedBuffer = buffer.sortWith(sortByTime)
    //rs.all.sort(timeComparator)
    //Collections.sort(rs.all, Comparator.comparing(Row::getTimestamp("request_time")))
    Ok(views.html.table(sortedBuffer))
  }

  def sortByTime(row1: Row, row2: Row) = {
    //logger.trace("comparing %s and %s".format(row1, row2))
    row1.getTimestamp("request_time").after(row2.getTimestamp("request_time"))
  }

  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  /**
   * Uses a custom FilePartHandler to return a type of "File" rather than
   * using Play's TemporaryFile class.  Deletion must happen explicitly on
   * completion, rather than TemporaryFile (which uses finalization to
   * delete temporary files).
   *
   * @return
   */
  private def handleFilePartAsFile: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType) =>
      val attr = PosixFilePermissions.asFileAttribute(util.EnumSet.of(OWNER_READ, OWNER_WRITE))
      val path: Path = Files.createTempFile("multipartBody", "tempFile", attr)
      val file = path.toFile
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          logger.info(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, file)
      }
  }

  /**
   * A generic operation on the temporary file that deletes the temp file after completion.
   */
  private def operateOnTempFile(file: File) = {
    val size = Files.size(file.toPath)
    logger.info(s"size = ${size}")
    Files.deleteIfExists(file.toPath)
    size
  }

  /**
   * Uploads a multipart file as a POST request.
   *
   * @return
   */
  def upload = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    val fileOption = request.body.file("Location:").map {
      case FilePart(key, filename, contentType, file) =>
        val description = request.body.asFormUrlEncoded.get("Description:").get.head
        logger.info(s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file description = $description")
        val imageUUID = java.util.UUID.randomUUID.toString
        val imageBytes = Files.readAllBytes(file.toPath)
        val metadata = ImageMetadata(description,Files.size(file.toPath).toInt,"webapp",imageUUID,DateTime.now)
        val json = Json.toJson(metadata).toString
        logger.info(s"Converted to json $json")
        val record = KafkaProducerRecord("upload_images", json, imageBytes)
        producer.send(record)
        val data = operateOnTempFile(file)
        s"$data. UUID = $imageUUID"
    }

    Ok(s"file size = ${fileOption.getOrElse("no file")}")
  }

}
object CassandraClient {
  private val cluster = Cluster.builder()
    .addContactPoint("localhost")
    .withPort(9042)
    .build()

  val session = cluster.connect()

  def getValueFromCassandraTable = {
    //TODO async with future
    session.execute("SELECT * FROM images.labels")
  }
}
