package tbank.academy.adapter.persistance

import cats.effect.unsafe.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.{Github, Link, Stackoverflow}

trait LinkRepositorySpec extends AnyFlatSpec with Matchers with RepositorySpec {
  behavior of "LinkRepository"

  it should "insert, get and delete links" in withContainers { container =>
    val repositories = mkRepository(container)

    val chatId = 1L
    val link   = Link("url1", "api.url1", Github, 24, Set("simple", "forum"), Set(chatId))

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)

      _ <- repositories.linkRepository.insertLink(chatId, link)

      links <- repositories.linkRepository.getLinks(chatId)
    } yield assert(links.exists(_.url == link.url))).unsafeRunSync()
  }

  it should "LinkRepository should update multiple links" in withContainers { container =>
    val repositories = mkRepository(container)

    val chatId = 2L
    val link1  = Link("url1", "api.url1", Stackoverflow, 12, Set.empty[String], Set(chatId), None)
    val link2  = Link("url2", "api.url", Stackoverflow, 23, Set.empty[String], Set(chatId), None)

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)
      _ <- repositories.linkRepository.insertLink(chatId, link1)
      _ <- repositories.linkRepository.insertLink(chatId, link2)

      _ <- repositories.linkRepository.updateCount(link1.url, 10)

      links <- repositories.linkRepository.getLinks(chatId)
    } yield assert(links.exists(_.url == link1.url) &&
      links.exists(_.processedCount == 10) &&
      links.size == 2)).unsafeRunSync()
  }

  it should "LinkRepository should update links and preserve tags" in withContainers { container =>
    val repositories = mkRepository(container)
    val chatId       = 3L
    val tags1        = Set("scala", "functional")
    val tags2        = Set("java", "oop")

    val link1 = Link("url1", "api.url1", Stackoverflow, 12, tags1, Set(chatId), None)
    val link2 = Link("url2", "api.url2", Stackoverflow, 24, tags2, Set(chatId), None)

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)
      _ <- repositories.linkRepository.insertLink(chatId, link1)
      _ <- repositories.linkRepository.insertLink(chatId, link2)

      links <- repositories.linkRepository.getLinks(chatId, "scala")
    } yield {
      assert(links.contains(link1) && links.size == 1)
    }).unsafeRunSync()
  }
}
