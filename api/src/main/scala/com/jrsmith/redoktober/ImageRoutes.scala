package com.jrsmith.redoktober

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.jrsmith.redoktober.Requests.{EmptyIdentifyObjectsReq, IdentifyObjectsReq, InvalidIndentifyObjectsReq}
import com.jrsmith.redoktober.Responses.{ImageResponse, ImagesResponse}
import com.jrsmith.redoktober.db.repositories.{ImageIdAndMetadataDTO, ImagesRepo}
import com.jrsmith.redoktober.domain.{LocalSource, RemoteSource}
import com.jrsmith.redoktober.services.{ImageService, ImageServiceImpl, ObjectDetectionServiceImpl}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

/** Request Objects sent to the API
 *
 */
object Requests{
  case class IdentifyObjectsReq(image: Option[String],
                                imageUrl: Option[String],
                                label: Option[String],
                                identifyObjects: Option[Boolean])

  object EmptyIdentifyObjectsReq extends Throwable("Please provide an image or imageUrl")
  object InvalidIndentifyObjectsReq extends Throwable("Please provide an image or an imageUrl but not both")
}

object Responses{
  case class ImagesResponse(images: List[ImageIdAndMetadataDTO])
  case class ImageResponse(image: ImageIdAndMetadataDTO)
}

class ImageRoutes(imageService: ImageService[Future]) extends FailFastCirceSupport{
  import io.circe.syntax._
  //Images path
  val ImagesPath = "images"

  // Little Hello World to make sure stuff is working
  val helloRoute = get{ complete("Hello world!") }

  /** Accepts a json payload [[IdentifyObjectsReq]] and persists the image from it.
   *
   * Some validation is done to verify that either image or imageUrl are present.
   * If the `identifyObjects` property is defined as true, object detection will occur
   * If the `identifyObjects` is undefined, object detection will not occur.
   * If a label is not supplied, one will be created in the Image Metadata object
   *
   * Tried using Akka case class validation but it was not working properly with Option types
   */
  lazy val createImageRoute: Route = extractLog { implicit log =>
    post {
      entity(as[IdentifyObjectsReq]) { req =>
        val result = req match {
          // Invalid because both image and imageUrl are supplied
          case IdentifyObjectsReq(Some(_), Some(_), _, _) => Future.failed(InvalidIndentifyObjectsReq)

          // Invalid because image nor imageUrl are supplied
          case IdentifyObjectsReq(None, None, _, _) => Future.failed(EmptyIdentifyObjectsReq)

          // Saving as a Local image
          case IdentifyObjectsReq(Some(image), _, labelOpt, detectOpt) =>
            imageService.saveImage(LocalSource(image), labelOpt, detectOpt)

          // Saving as a remote image
          case IdentifyObjectsReq(_, Some(imageUrl), labelOpt, detectOpt) =>
            imageService.saveImage(RemoteSource(imageUrl), labelOpt, detectOpt)
        }

        onComplete(result) {
          case Success(s) =>
            log.info(s"Successfully persisted image ${s.id}")
            complete(ImageResponse(s))

          case Failure(ex: InvalidIndentifyObjectsReq.type) =>
            log.warning(ex, "Invalid request when attempting to save image")
            complete(StatusCodes.BadRequest, ex.getMessage)

          case Failure(ex: EmptyIdentifyObjectsReq.type) =>
            log.warning(ex, "Invalid request when attempting to save image")
            complete(StatusCodes.BadRequest, ex.getMessage)

          case Failure(er) =>
            log.error(er, "Error occurred while trying to persist image")
            complete(StatusCodes.InternalServerError, er.getMessage)
        }
      }
    }
  }

  /** Gets all images OR Gets images with a specific set of objects if objects parameter is supplied
   *
   */
  lazy val getImagesRoute: Route = extractLog{ implicit log =>
    get {
      parameter("objects".optional){ objectsOpt =>

        // Taking output from objects param, spliting by comma and trimming.
        val imagesFut = objectsOpt.map(_.split(",").map(_.trim).toList) match {
          case Some(objects) =>
            log.info(s"Received query to search for images with objects ${objects.mkString(",")}")
            imageService.searchImages( objects )

          case None =>
            log.info("Retrieving all images")
            imageService.allImages()
        }

        onComplete( imagesFut ) {
          case Success(images) =>
            log.info(s"Get /${ImagesPath} success")
            complete( ImagesResponse(images))

          case Failure(er) =>
            log.error(er, "There was an error getting all images.")
            complete(StatusCodes.InternalServerError, s"An error occurred due to $er")
        }
      }
    }
  }

  /** Gets a single image by ID
   *
   * @param imageId UUID
   * @return
   */
  def getImageRoute(imageId: UUID): Route = extractLog{ implicit log =>
    get{
      onComplete( imageService.imageById(imageId) ) {
        // Found the image
        case Success(Some(image)) =>
          log.info(s"Retrieved image id: ${imageId}")
          complete( ImageResponse(image) )

        // Successfully connected to DB, but row was not found
        case Success(None) =>
          log.warning(s"Image ${imageId} requested but was not found")
          complete( StatusCodes.NotFound )

        // Error occurred
        case Failure(er) =>
          log.error(er,s"There was an error retrieving $imageId")
          complete( StatusCodes.InternalServerError, er.getMessage)
      }
    }
  }

  /** All the routes that servce Image requests
   *
   */
  lazy val routes: Route =
    concat(
      pathSingleSlash( helloRoute ),
      path( ImagesPath )( getImagesRoute ),
      path( ImagesPath )( createImageRoute),
      path( ImagesPath / JavaUUID)( getImageRoute )
    )
}