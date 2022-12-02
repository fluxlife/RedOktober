package tests.unit.services

import cats.effect.unsafe.implicits.global
import com.jrsmith.redoktober.db.repositories.ImageIdAndMetadataDTO
import com.jrsmith.redoktober.domain.ImageSource
import com.jrsmith.redoktober.services.ImageService
import tests.unit.db.repositories.ImageRepositoryGen

import cats.effect.unsafe.implicits.global

import java.util.UUID
import scala.concurrent.Future

object ImageServiceGen {
  val imageServiceGen = for{
    findAllImagesIO <- ImageRepositoryGen.findAllImagesGen
    findImagesByIdIO <- ImageRepositoryGen.findImageByIdGen
    findAllImagesByObjectIO <- ImageRepositoryGen.findAllImagesByObjectsGen
    saveImageIO <- ImageRepositoryGen.saveImageGen
  } yield new ImageService[Future]{
    /** Retrieve all Images
     *
     * @return A list of all images with metadata
     */
    override def allImages(): Future[List[ImageIdAndMetadataDTO]] = findAllImagesIO.unsafeToFuture()

    /** Finds all images that contain certain objects
     *
     * @param objects
     * @return A list of images contraining certain objects. Can be empty
     */
    override def searchImages(objects: List[String]): Future[List[ImageIdAndMetadataDTO]] =
      findAllImagesByObjectIO.unsafeToFuture()

    /** Find an image by UUID
     *
     * @param uuid
     * @return An Option of image
     */
    override def imageById(uuid: UUID): Future[Option[ImageIdAndMetadataDTO]] = findImagesByIdIO.unsafeToFuture()

    /** Persist an image to the store
     *
     * @param imageSource          The image source whether its Local or Remote
     * @param labelOpt             Optional label
     * @param detectObjectsFlagOpt The flag determining whether object detection should be run
     * @return The image and metadata persisted to the store
     */
    override def saveImage(imageSource: ImageSource,
                           labelOpt: Option[String],
                           detectObjectsFlagOpt: Option[Boolean]): Future[ImageIdAndMetadataDTO] =
      saveImageIO.unsafeToFuture()
  }
}
