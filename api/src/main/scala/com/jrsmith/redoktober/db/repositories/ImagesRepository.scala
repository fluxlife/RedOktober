package com.jrsmith.redoktober.db.repositories

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{IO, Resource}
import com.jrsmith.redoktober.domain.{DetectedObject, Image, LocalSource, RemoteSource}
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie._
import doobie.hikari.HikariTransactor
import doobie.postgres.circe.jsonb.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import java.util.UUID

/** A DTO used to get data out of the database
 *
 * @param id UUID of the image
 * @param metadata Metadata of the image represented as Json
 */
case class ImageIdAndMetadataDTO(id: UUID, metadata: Json)

trait ImagesRepository[F[_]] {
  /** Get all images from the store
   *
   * @return List of images and metadata
   */
  def findAllImages(): F[List[ImageIdAndMetadataDTO]]

  /** Get all the images that contain the objects
   *
   * @param objects A list of detected objects in the mage
   * @return List of images and metadata
   */
  def findAllImagesByObjects(objects: NonEmptyList[String]): F[List[ImageIdAndMetadataDTO]]

  /** Find an image by Id
   *
   * @param UUID The UUID of the image
   * @return An option of image and metadata
   */
  def findImageById(UUID: UUID): F[Option[ImageIdAndMetadataDTO]]

  /** Save an image to the store
   *
   * @param image
   * @return The image and metadata
   */
  def saveImage(image: Image): F[ImageIdAndMetadataDTO]
}

class ImagesRepo(transactor: Resource[IO, HikariTransactor[IO]]) extends ImagesRepository[IO]{

  /** Keeping Queries separate so that these queries can be tested easier.
   *
   */
  object Queries{
    val findAllImages =
      sql"SELECT id, metadata FROM images"
        .query[ImageIdAndMetadataDTO]

    def findAllImagesWithObjects(objects: NonEmptyList[String]) = {
      val frags = fr"""
          SELECT DISTINCT i.id, i.metadata
          FROM redoktober.images i
          CROSS JOIN jsonb_array_elements(i.metadata->'detectedObjects')
          WHERE """ ++ Fragments.in(fr"value->>'name'", objects)

      frags.query[ImageIdAndMetadataDTO]
    }

    def findImageById(uuid: UUID) =
      sql"SELECT id, metadata FROM images where id = $uuid"
        .query[ImageIdAndMetadataDTO]

    def saveImage(imageId: UUID, imageSource: String, metadata: Json) = {
      sql"""INSERT INTO images (id, image_source, metadata)
            VALUES ($imageId, $imageSource, $metadata)
         """
        .update
    }

  }

  /** Get all images from the store
   *
   * @return List of images and metadata
   */
  override def findAllImages(): IO[List[ImageIdAndMetadataDTO]] = {
    transactor.use{ xa =>
      Queries.findAllImages
        .to[List]
        .transact(xa)
    }
  }

  /** Get all the images that contain the objects
   *
   * @param objects A list of detected objects in the mage
   * @return List of images and metadata
   */
  override def findAllImagesByObjects(objects: NonEmptyList[String]): IO[List[ImageIdAndMetadataDTO]] = {

    transactor.use{ xa =>
      Queries.findAllImagesWithObjects(objects)
        .to[List]
        .transact(xa)
    }
  }

  /** Find an image by Id
   *
   * @param UUID The UUID of the image
   * @return An option of image and metadata
   */
  override def findImageById(uuid: UUID): IO[Option[ImageIdAndMetadataDTO]] = {
    transactor.use{ xa =>
      Queries.findImageById(uuid)
        .option
        .transact(xa)
    }
  }

  /** Save an image to the store
   *
   * @param image
   * @return The image and metadata
   */
  override def saveImage(image: Image): IO[ImageIdAndMetadataDTO] = {
    val imageSource = image.source match {
      case LocalSource(bytes) => bytes
      case RemoteSource(url) => url
    }

    val metadataJson = image.metadata.asJson

    transactor.use{ xa =>
      Queries.saveImage(image.id, imageSource, metadataJson)
        .withUniqueGeneratedKeys[ImageIdAndMetadataDTO]("id","metadata")
        .transact(xa)
    }
  }

}
