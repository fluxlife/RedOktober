package tests.unit.domain

import com.jrsmith.redoktober.domain.{Image, ImageMetadata, RemoteSource}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ImageTests extends AnyFlatSpec with Matchers{

  "ImageMetadata" should "generate a label if one is not provided" in {
    val imageMetadata = ImageMetadata(None,None)
    assert( !imageMetadata.label.isBlank )
  }

  it should "set the detectObjectsFlag to false if None is provided" in {
    val imageMetadata = ImageMetadata(None, None)
    assert( !imageMetadata.detectObjects )
  }

  "Image" should "add the imageUrl to metadata if it has a remote source" in {
    val url = "https://someUrl.com"
    val remoteSource = RemoteSource(url)
    val imageMetadata = ImageMetadata(None, Some(true))

    imageMetadata.imageUrl shouldBe(None)

    val image = Image(remoteSource, imageMetadata)
    
    image.metadata.imageUrl shouldBe( Some(url) )
  }

}
