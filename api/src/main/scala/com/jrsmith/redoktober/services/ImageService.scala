package com.jrsmith.redoktober.services

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.jrsmith.redoktober.db.repositories.{ImageIdAndMetadataDTO, ImagesRepository}
import doobie._
import cats.data.NonEmptyList
import cats.effect.IO
import doobie.implicits._
import cats.effect.unsafe.implicits.global
import com.jrsmith.redoktober.domain.{Image, ImageMetadata, ImageSource}
import com.typesafe.scalalogging.LazyLogging

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ErrorDetectingObjects extends Throwable("There was an error detecting objects")

/** Image Service handling all image related requests
 *
 * Allows monadic type parameter so an effect type can be configured
 *
 * @tparam F Something monadic (but not technically a monad)
 */
trait ImageService[F[_]] {
  /** Retrieve all Images
   *
   * @return A list of all images with metadata
   */
  def allImages(): F[List[ImageIdAndMetadataDTO]]

  /** Finds all images that contain certain objects
   *
   * @param objects
   * @return A list of images contraining certain objects. Can be empty
   */
  def searchImages(objects: List[String]): F[List[ImageIdAndMetadataDTO]]

  /** Find an image by UUID
   *
   * @param uuid
   * @return An Option of image
   */
  def imageById(uuid: UUID): F[Option[ImageIdAndMetadataDTO]]

  /** Persist an image to the store
   *
   * @param imageSource The image source whether its Local or Remote
   * @param labelOpt Optional label
   * @param detectObjectsFlagOpt The flag determining whether object detection should be run
   * @return The image and metadata persisted to the store
   */
  def saveImage(imageSource: ImageSource,
                labelOpt: Option[String],
                detectObjectsFlagOpt: Option[Boolean]): F[ImageIdAndMetadataDTO]
}

/** Impelemtation of [[ImageService]] with a Future effect type for better Akka integration
 *
 * @param imagesRepository A repository of images
 * @param objectDetectionService The service to detect objects in images
 * @param system implicit actor system because ... Akka
 */
class ImageServiceImpl( imagesRepository: ImagesRepository[IO],
                        objectDetectionService: ObjectDetectionService )
                      (implicit system: ActorSystem)

  extends ImageService[Future] with LazyLogging {

  /**
   * @return A list of all images with metadata
   */
  override def allImages(): Future[List[ImageIdAndMetadataDTO]] = imagesRepository.findAllImages().unsafeToFuture()

  /**
   * @param objects
   * @return A list of images contraining certain objects. Can be empty
   */
  override def searchImages(objects: List[String]): Future[List[ImageIdAndMetadataDTO]] = {
    val io = NonEmptyList.fromList(objects) match {
      case Some(nelObjects) => imagesRepository.findAllImagesByObjects(nelObjects)
      case None => IO.raiseError(new IllegalArgumentException("Objects array is empty"))
    }
    io.unsafeToFuture()
  }

  /**
   * @param uuid
   * @return An Option of image
   */
  override def imageById(uuid: UUID): Future[Option[ImageIdAndMetadataDTO]] =
    imagesRepository.findImageById(uuid).unsafeToFuture()

  /**
   * @param imageSource The image source whether its Local or Remote
   * @param labelOpt    Optional label
   * @param detectObjectsOpt
   * @return The image and metadata persisted to the store
   */
  override def saveImage(imageSource: ImageSource, labelOpt: Option[String], detectObjectsOpt: Option[Boolean]): Future[ImageIdAndMetadataDTO] = {
    Source
      .single( Image(imageSource, ImageMetadata(labelOpt, detectObjectsOpt) ) )
      .map{ image =>

        val detectedObjectsTry =
          if(image.metadata.detectObjects)
            objectDetectionService.detectObjects( image.source )
          else
            Try(List.empty)

        detectedObjectsTry match {
          case Success(detectedObjects) =>
            val updatedMetadata = image.metadata.copy(detectedObjects = detectedObjects)
            image.copy( metadata = updatedMetadata)

          case Failure(exception) =>
            logger.error("An error occurred detecting objects ", exception)
            throw ErrorDetectingObjects
        }
      }
      .mapAsync(1){ image =>
        imagesRepository.saveImage(image).unsafeToFuture()
      }
      .runWith(Sink.head)
  }
}