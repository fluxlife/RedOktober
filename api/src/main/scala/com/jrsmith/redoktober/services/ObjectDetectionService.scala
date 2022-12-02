package com.jrsmith.redoktober.services

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.cloud.vision.v1.{AnnotateImageRequest, Feature, ImageAnnotatorClient, ImageAnnotatorSettings, ImageSource => GoogleImageSource}
import com.google.cloud.vision.v1.{Image => GoogleImage}
import com.google.protobuf.ByteString
import com.jrsmith.redoktober.config.AppConfig
import com.jrsmith.redoktober.domain.{DetectedObject, LocalSource, RemoteSource, Vertex, ImageSource => ROImageSource}

import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.chaining._
import scala.language.implicitConversions

trait ObjectDetectionService {
  /** Detects objects in an image
   *
   * @param imageSource [[ROImageSource]]
   * @return Try[List[DetectedObject]] Try of List of detected objects
   */
  def detectObjects(imageSource: ROImageSource): Try[List[DetectedObject]]
}

object ObjectDetectionServiceImpl extends ObjectDetectionService {

  /**
   * Normally Google credentials are either set by env var or google cli.
   * Creds manually loaded by AppConfig and set here/
   */
  private val DefaultImageAnnotatorSettings =
    ImageAnnotatorSettings.newBuilder()
      .setCredentialsProvider( FixedCredentialsProvider.create( AppConfig.GoogleCreds ) )
      .build()

  // According to docs, only one instance of this should be created
  // https://cloud.google.com/vision/docs/object-localizer#detect_objects_in_a_local_image
  val GoogleImageAnnotatorClient = ImageAnnotatorClient.create(DefaultImageAnnotatorSettings)

  /**
   * Converted Java example to scala from here:
   * (https://cloud.google.com/vision/docs/object-localizer#detect_objects_in_a_local_image)
   *
   * @param image Google Image object
   * @return Try[List[DetectedObject]] Try of List of detected objects
   */
  private def detectObjects(image: GoogleImage): Try[List[DetectedObject]] = Try{
    val request = AnnotateImageRequest.newBuilder()
      .addFeatures(Feature.newBuilder().setType(Feature.Type.OBJECT_LOCALIZATION))
      .setImage(image)
      .build()

    GoogleImageAnnotatorClient
      .batchAnnotateImages( List(request).asJava )
      .getResponsesList.asScala
      .flatMap(e => e.getLocalizedObjectAnnotationsList.asScala )
      .map { e =>
        val bbBoxVertices = e.getBoundingPoly.getNormalizedVerticesList.asScala.map(v => Vertex(v.getX, v.getY))
        DetectedObject(e.getName, e.getScore, bbBoxVertices.toVector)
      }
      .toList
  }

  /**
   * Takes an image represented as a base64 encoded string and utilizes Google's
   * Vision API to detect objects.
   *
   * @param base64Image Base64 encoded bytes of image
   * @return [[Try[List[DetectedObject]] a list of detected objects
   */
  private def detectLocalImage(base64Image: String): Try[List[DetectedObject]] = {
    base64Image
      .pipe( java.util.Base64.getDecoder.decode )
      .pipe( ByteString.copyFrom )
      .pipe( GoogleImage.newBuilder().setContent(_).build() )
      .pipe( detectObjects )
  }

  /**
   * Takes an imageUrl and utilizes Google's
   * Vision API to detect objects.
   *
   * @param imageUrl URL to an image
   * @return [[Try[List[DetectedObject]] a list of detected objects
   */
  private def detectRemoteImage(imageUrl: String): Try[List[DetectedObject]] = {
    imageUrl
      .pipe( GoogleImageSource.newBuilder().setImageUri(_).build() )
      .pipe( GoogleImage.newBuilder().setSource(_).build() )
      .pipe( detectObjects )
  }

  /** Detects Objects in an image
   *
   * @param imageSource [[ROImageSource]]
   * @return Try[List[DetectedObject]] Try of List of detected objects
   */
  override def detectObjects(imageSource: ROImageSource): Try[List[DetectedObject]] =
    imageSource match {
      case LocalSource(bytes) => detectLocalImage(bytes)
      case RemoteSource(url) => detectRemoteImage(url)
    }
}
