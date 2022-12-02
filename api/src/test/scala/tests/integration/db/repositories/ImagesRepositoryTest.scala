package tests.integration.db.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import com.jrsmith.redoktober.config.AppConfig
import com.jrsmith.redoktober.db.migrations.FlywayRunner
import com.jrsmith.redoktober.db.repositories.ImagesRepo
import doobie.util.transactor.Transactor
import io.circe.Json
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

class ImagesRepositoryTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with doobie.scalatest.IOChecker{

  val transactor = Transactor.fromDriverManager[IO](
    AppConfig.HikariConf.driver, AppConfig.HikariConf.url, AppConfig.HikariConf.user, AppConfig.HikariConf.password
  )

  override def beforeAll(): Unit = FlywayRunner.run()

  val repo = new ImagesRepo(AppConfig.DbTransactor)

  "ImagesRepository" should "have valid queries" in {
    check( repo.Queries.findAllImages )
    check( repo.Queries.saveImage( UUID.randomUUID(),"",Json.fromInt(1)) )
    check( repo.Queries.findImageById( UUID.randomUUID() ) )
    check( repo.Queries.findAllImagesWithObjects( NonEmptyList.one("cats") ))
  }



}
