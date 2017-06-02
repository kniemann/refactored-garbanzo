import com.github.nscala_time.time.Imports.DateTime
import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._


case class ImageMetadata(imageName: String,
                         imageSize: Int,
                         source: String,
                         requestTime: DateTime)

implicit val imageMetadataReads: Reads[ImageMetadata] = (
  (JsPath \ "imageName").read[String] and
    (JsPath \ "imageSize").read[Int] and
    (JsPath \ "source").read[String] and
    (JsPath \ "requestTime").read[DateTime]
  )(ImageMetadata.apply _)
implicit val imageMetadataWrites: Writes[ImageMetadata] = (
  (JsPath \ "imageName").write[String] and
    (JsPath \ "imageSize").write[Int] and
    (JsPath \ "source").write[String] and
    (JsPath \ "requestTime").write[DateTime]
  )(unlift(ImageMetadata.unapply))