package tests.unit.services

import com.jrsmith.redoktober.domain.{DetectedObject, ImageSource}
import com.jrsmith.redoktober.services.ObjectDetectionService
import org.scalacheck.Gen
import tests.unit.domain.ImageGen.detectedObjectGen

import scala.util.{Failure, Success, Try}

object ObjectDetectionServiceGen {

  object ObjectDetectionGenError extends Throwable("generated error")

  val objectDetectionResultGen =
    Gen.oneOf(
      Gen.listOf( detectedObjectGen ).map(s => Success(s)),
      Gen.map(_ => Failure( ObjectDetectionGenError ))
    )

  val objectDetectionServiceGen = for{
    objDetectionResult <- objectDetectionResultGen
  } yield new ObjectDetectionService{
    /** Detects objects in an image
     *
     * @param imageSource [[ROImageSource]]
     * @return Try[List[DetectedObject]] Try of List of detected objects
     */
    override def detectObjects(imageSource: ImageSource): Try[List[DetectedObject]] = objDetectionResult
  }


}
