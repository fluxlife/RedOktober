package com.jrsmith.redoktober

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import cats._
import cats.implicits._
import cats.data.{Kleisli, NonEmptyList}
import cats.effect.{ExitCode, IO, IOApp}
import com.jrsmith.redoktober.config.AppConfig
import com.jrsmith.redoktober.db.migrations.FlywayRunner
import com.jrsmith.redoktober.db.repositories.{ImageIdAndMetadataDTO, ImagesRepo, ImagesRepository}
import com.jrsmith.redoktober.domain.{Image, ImageMetadata, ImageSource}
import com.jrsmith.redoktober.services.{ErrorDetectingObjects, ImageService, ObjectDetectionService, ObjectDetectionServiceImpl}
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl._
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.server.Router
import org.http4s.circe._

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class ImageServiceCatsEffectImpl( imagesRepository: ImagesRepository[IO],
                                  objectDetectionService: ObjectDetectionService)
  extends ImageService[IO]with LazyLogging {

  override def allImages(): IO[List[ImageIdAndMetadataDTO]] = imagesRepository.findAllImages()

  override def searchImages(objects: List[String]): IO[List[ImageIdAndMetadataDTO]] = {
    NonEmptyList.fromList(objects) match {

      case Some(nelObjects) => imagesRepository.findAllImagesByObjects( nelObjects )

      case None => IO.raiseError(new IllegalArgumentException("Objects is empty"))

    }
  }

  override def imageById(uuid: UUID): IO[Option[ImageIdAndMetadataDTO]] = imagesRepository.findImageById(uuid)

  override def saveImage(imageSource: ImageSource,
                         labelOpt: Option[String],
                         detectObjectsFlagOpt: Option[Boolean]): IO[ImageIdAndMetadataDTO] = {


    IO.pure(Image(imageSource, ImageMetadata(labelOpt, detectObjectsFlagOpt)))
      .flatMap { image =>
        val detectedObjectsTry =
          if (image.metadata.detectObjects)
            objectDetectionService.detectObjects(image.source)
          else
            Try(List.empty)

        detectedObjectsTry match {
          case Success(detectedObjects) =>
            val updatedMetadata = image.metadata.copy(detectedObjects = detectedObjects)
            IO.pure( image.copy(metadata = updatedMetadata) )

          case Failure(exception) =>
            logger.error("An error occurred detecting objects ", exception)
            IO.raiseError( ErrorDetectingObjects )
        }
      }
      .flatMap(image => imagesRepository.saveImage(image))
  }
}

object Http4sRoutes {

  val ImagePath = "images"
  object ObjectQueryParamMatcher extends QueryParamDecoderMatcher[String]("objects")

  def apply(imageService: ImageService[IO]): HttpRoutes[IO] = {
    val dsl = Http4sDsl[IO]
    import dsl._
    HttpRoutes.of[IO] {

      case GET -> Root => Ok("Hello world")

      // Returns HTTP 200 OK with a JSON response containing all image metadata.
      case GET -> Root / ImagePath =>
        imageService.allImages().flatMap(r => Ok(r.asJson))

//
//      /*
//      Returns a HTTP 200 OK with a JSON response body containing only images that have the detected objects
//      specified in the query parameter.
//      */
//      case GET -> Root / ImagePath ?: ObjectQueryParamMatcher => ???
//
//      // Returns HTTP 200 OK with a JSON response containing image metadata for the specified image.
//      case GET -> Root / ImagePath / UUIDVar(imageId) => ???
//
//      /*
//      Send a JSON request body including an image file or URL, an optional label for the image, and an
//      optional field to enable object detection.
//
//      Returns a HTTP 200 OK with a JSON response body including the image data, its label (generate one if the user
//      did not provide it), its identifier provided by the persistent data store, and any objects
//      detected (if object detection was enabled).
//      */
//      case POST -> Root / ImagePath => ???
    }
  }
}

object Http4sAttempt extends IOApp{

  val app: Kleisli[IO, Request[IO], Response[IO]] = {
    val imageRepo = new ImagesRepo(AppConfig.DbTransactor)

    val imageService = new ImageServiceCatsEffectImpl(imageRepo, ObjectDetectionServiceImpl)

    Router(
      "/" -> Http4sRoutes( imageService )
    ).orNotFound
  }

  override def run(args: List[String]): IO[ExitCode] = {
    FlywayRunner.run()
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp( app )
      .resource
      .useForever
      .as(ExitCode.Success)
  }

}
