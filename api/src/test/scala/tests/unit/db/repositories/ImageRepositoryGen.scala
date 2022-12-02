package tests.unit.db.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import com.jrsmith.redoktober.db.repositories.{ImageIdAndMetadataDTO, ImagesRepository}
import com.jrsmith.redoktober.domain.Image
import org.scalacheck.Gen
import tests.unit.domain.ImageGen.imageMetadataGen
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._

import java.util.UUID

object ImageRepositoryGen {
  object ImageRepositoryGenError extends Throwable("generated error")

  val imageIdAndMetadataDTOGen = for{
    id <- Gen.uuid
    metadata <- imageMetadataGen
  } yield ImageIdAndMetadataDTO(id, metadata.asJson)

  val findAllImagesGen =
    Gen.oneOf(
      Gen.listOf(imageIdAndMetadataDTOGen).map(IO.pure),
      Gen.map(_ => IO.raiseError(ImageRepositoryGenError))
    )

  val findImageByIdGen =
    Gen.oneOf(
      Gen.option(imageIdAndMetadataDTOGen).map(i => IO.pure(i)),
      Gen.map(_ => IO.raiseError(ImageRepositoryGenError))
    )

  val findAllImagesByObjectsGen =
    Gen.oneOf(
      Gen.listOf(imageIdAndMetadataDTOGen).map(IO.pure),
      Gen.map(_ => IO.raiseError(ImageRepositoryGenError))
    )

  val saveImageGen =
    Gen.oneOf(
      imageIdAndMetadataDTOGen.map(IO.pure),
      Gen.map(_ => IO.raiseError(ImageRepositoryGenError))
    )

  val imageRepositoryGen = for{
    findAllImagesIO <- findAllImagesGen
    findImagesByIdIO <- findImageByIdGen
    findAllImagesByObjectIO <- findAllImagesByObjectsGen
    saveImageIO <- saveImageGen
  } yield new ImagesRepository[IO] {
    /** Get all images from the store
     *
     * @return List of images and metadata
     */
    override def findAllImages(): IO[List[ImageIdAndMetadataDTO]] = findAllImagesIO

    /** Get all the images that contain the objects
     *
     * @param objects A list of detected objects in the mage
     * @return List of images and metadata
     */
    override def findAllImagesByObjects(objects: NonEmptyList[String]): IO[List[ImageIdAndMetadataDTO]] = findAllImagesByObjectIO

    /** Find an image by Id
     *
     * @param UUID The UUID of the image
     * @return An option of image and metadata
     */
    override def findImageById(UUID: UUID): IO[Option[ImageIdAndMetadataDTO]] = findImagesByIdIO

    /** Save an image to the store
     *
     * @param image
     * @return The image and metadata
     */
    override def saveImage(image: Image): IO[ImageIdAndMetadataDTO] = saveImageIO
  }




}
