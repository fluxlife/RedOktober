package tests.unit.domain

import com.jrsmith.redoktober.domain.{DetectedObject, Image, ImageMetadata, LocalSource, RemoteSource, Vertex}
import org.scalacheck.Gen

object ImageGen {
  val vertexGen = for{
    x <- Gen.double.map(_.toFloat)
    y <- Gen.double.map(_.toFloat)
  } yield Vertex(x,y)

  val detectedObjectGen = for{
    name <- Gen.alphaStr
    score <- Gen.double.map(_.toFloat)
    boundingBox <- Gen.listOfN(4, vertexGen)
  } yield DetectedObject(name, score, boundingBox.toVector)

  val localSourceGen = for{
    bytes <- Gen.alphaStr
  } yield LocalSource(bytes)

  val remoteSourceGen = for{
    url <- Gen.oneOf(Seq("http://google.com", "http://altavista.com"))
  } yield RemoteSource(url)

  val imageSourceGen = Gen.oneOf(localSourceGen, remoteSourceGen)

  val imageMetadataGen = for{
    label <- Gen.alphaStr
    detectedObjects <- Gen.oneOf(Gen.map(_ => List.empty), Gen.listOf(detectedObjectGen))
    imageUrl <- Gen.option(Gen.alphaStr)
    detectObjects <- Gen.oneOf(Seq(true, false))
  } yield {
    if(!detectObjects)
      ImageMetadata(label, List.empty, imageUrl, detectObjects)
    else
      ImageMetadata(label, detectedObjects, imageUrl, detectObjects)
  }

  val imageGen = for{
    id <- Gen.uuid
    source <- imageSourceGen
    metadata <- imageMetadataGen
  } yield Image(id, source, metadata)
}
