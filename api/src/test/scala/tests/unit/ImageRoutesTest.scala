package tests.unit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.jrsmith.redoktober.ImageRoutes
import com.jrsmith.redoktober.Requests.IdentifyObjectsReq
import com.jrsmith.redoktober.domain.ImageSource
import com.jrsmith.redoktober.services.ImageService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest._
import matchers._
import org.scalacheck.Gen
import org.scalatest.propspec.AnyPropSpec
import org.scalacheck._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatestplus.scalacheck.Checkers.check
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import tests.unit.domain.ImageGen
import tests.unit.services.ImageServiceGen

import java.util.UUID
import scala.collection.immutable._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import io.circe.generic.auto._

class ImageRoutesTest extends AnyFlatSpec with should.Matchers with ScalatestRouteTest with FailFastCirceSupport{

  "GET /images" should "return the appropriate status code" in {

    forAll( ImageServiceGen.imageServiceGen ){ (imageService: ImageService[Future]) =>
      val imageServiceResponseFut = imageService.allImages()
        .map(_ => StatusCodes.OK)
        .recover{ case _ => StatusCodes.InternalServerError }

      val imageServiceResponse = Await.result(imageServiceResponseFut,5.seconds)

      val imageRoutes = new ImageRoutes( imageService )

      Get(s"/${imageRoutes.ImagesPath}") ~> imageRoutes.getImagesRoute ~> check {
        status shouldEqual imageServiceResponse
      }
    }

  }

  "GET /images?objects=parms" should "return the appropriate status code" in {
    forAll(ImageServiceGen.imageServiceGen) { (imageService: ImageService[Future]) =>
      val objects=List("bikes","flowers")
      val imageServiceResponseFut = imageService.searchImages(objects)
        .map(_ => StatusCodes.OK)
        .recover { case _ => StatusCodes.InternalServerError }

      val imageServiceResponse = Await.result(imageServiceResponseFut, 5.seconds)

      val imageRoutes = new ImageRoutes(imageService)

      Get(s"/${imageRoutes.ImagesPath}?objects=${objects.mkString(",")}") ~> imageRoutes.getImagesRoute ~> check {
        status shouldEqual imageServiceResponse
      }
    }
  }

  "GET /images/{imageId}" should "return the appropriate status code" in {
    forAll(ImageServiceGen.imageServiceGen, Gen.uuid) { (imageService: ImageService[Future], imageId: UUID) =>

      val imageServiceResponseFut = imageService.imageById(imageId)
        .map {
          case Some(_) => StatusCodes.OK
          case None => StatusCodes.NotFound
        }
        .recover { case _ => StatusCodes.InternalServerError }

      val imageServiceResponse = Await.result(imageServiceResponseFut, 5.seconds)

      val imageRoutes = new ImageRoutes(imageService)

      Get(s"/${imageRoutes.ImagesPath}/${imageId}") ~> imageRoutes.getImageRoute(imageId) ~> check {
        status shouldEqual imageServiceResponse
      }
    }
  }

  "POST /images" should "return the appropriate status code" in {
    forAll(
      ImageServiceGen.imageServiceGen,
      ImageGen.imageSourceGen,
      Gen.option(Gen.alphaStr),
      Gen.option(Gen.oneOf(Seq(true, false)))
    ) { (imageService: ImageService[Future], imageSource: ImageSource, label: Option[String], flag: Option[Boolean]) =>

      val imageServiceResponseFut = imageService.saveImage(imageSource, label, flag)
        .map(_ =>StatusCodes.OK )
        .recover { case _ => StatusCodes.InternalServerError }

      val imageServiceResponse = Await.result(imageServiceResponseFut, 5.seconds)

      val imageRoutes = new ImageRoutes(imageService)

      val reqBody = IdentifyObjectsReq(Some("str"),None, label, flag)

      Post(s"/${imageRoutes.ImagesPath}", reqBody) ~> imageRoutes.createImageRoute ~> check {
        status shouldEqual imageServiceResponse
      }
    }
  }

}