package com.jrsmith.redoktober.domain

import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.UUID

case class Vertex(x: Float, y: Float)

/** An object that has been detected in an image
 *
 * @param name The name of the object detected
 * @param score The confidence the ML model has in determining it's a match with the object detected
 * @param boundingBox The coordinates the object is located in within the image
 */
case class DetectedObject(name: String, score: Float, boundingBox: Vector[Vertex])

/** An image can either be a local set of bytes or an URL pointing to an image
 *
 */
sealed trait ImageSource

/** An actual image
 *
 * @param bytes Encoded base64 string of image bytes
 */
case class LocalSource(bytes: String) extends ImageSource

/** A reference to an external image
 *
 * @param url The URL location of the image
 */
case class RemoteSource(url: String) extends ImageSource

object ImageMetadata{

  /** Creates a default label for an image
   *
   * @return ImageReceivedAt suffixed with an ISO8601 Timestamp
   */
  private def generateLabel(): String = {
    val receivedTs = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    s"ImageReceivedAt $receivedTs"
  }

  /** Alternate constructor for ImageMetadata
   *
   * @param optLabel The optional label for the Image
   * @param detectObjectsFlag The optional flag for whether objects should be detected or note
   * @return ImageMetadata object
   */
  def apply(optLabel: Option[String],
            detectObjectsFlag: Option[Boolean] = None): ImageMetadata = {
    val label = optLabel.getOrElse(generateLabel())
    ImageMetadata(label, List.empty, None, detectObjectsFlag.getOrElse(false))
  }
}

/** Image Metadata for an image
 *
 * @param label The label assigned to an image
 * @param detectedObjects List of objects found in the image. May be empty
 * @param imageUrl Optional image url
 * @param detectObjects A flag representing whether objects should be detected in the image or not.
 */
case class ImageMetadata(label: String,
                         detectedObjects: List[DetectedObject],
                         imageUrl: Option[String],
                         detectObjects: Boolean)

object Image{
  /** Alternate constructor for Image
   *
   * @param source The ImageSource (Local or Remote)
   * @param metadata The metadata of the image
   * @return Image object
   */
  def apply(source: ImageSource, metadata: ImageMetadata): Image = {
    val uuid = UUID.randomUUID()
    val updatedMetadata =source match {
      case LocalSource(_) => metadata
      case RemoteSource(url) => metadata.copy(imageUrl = Some(url))
    }
    Image(uuid, source, updatedMetadata)
  }
}

/** Represents an Image
 *
 * @param id UUID of the image
 * @param source Whether the image is local or hosted remotely
 * @param metadata The metadata of the image
 */
case class Image(id: UUID, source: ImageSource, metadata: ImageMetadata)
