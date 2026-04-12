package tbank.academy.adapter.persistance

import cats.effect.unsafe.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.{Github, Link, Stackoverflow}

import java.time.Instant

trait LinkRepositorySpec extends AnyFlatSpec with Matchers with RepositorySpec {
  behavior of "LinkRepository"

  it should "insert, get and delete links" in withContainers { container =>
    val repositories = mkRepository(container)

    val chatId = 1L
    val link   = Link(
      url = "https://github.com/syoumzic/SimpleForum",
      apiUrl = "https://api.github.com/repo/syoumzic/SimpleForum",
      site = Github,
      tags = Set("simple", "forum"),
      chatIds = Set(chatId),
      lastUpdate = None
    )

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)

      _ <- repositories.linkRepository.insertLink(chatId, link)

      links <- repositories.linkRepository.getLinks(chatId)
    } yield assert(links.exists(_.url == link.url))).unsafeRunSync()
  }

  it should "LinkRepository should update multiple links" in withContainers { container =>
    val repositories = mkRepository(container)

    val chatId     = 2L
    val updateTime = Instant.ofEpochMilli(1776365914822L)
    val link1      = Link("url1", "api.url1", Stackoverflow, Set.empty[String], Set(chatId), None)
    val link2      = Link("url2", "api.url", Stackoverflow, Set.empty[String], Set(chatId), None)

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)
      _ <- repositories.linkRepository.insertLink(chatId, link1)
      _ <- repositories.linkRepository.insertLink(chatId, link2)

      updatedLink1 = link1.copy(lastUpdate = Some(updateTime))
      _ <- repositories.linkRepository.updateLinks(List(updatedLink1))

      links <- repositories.linkRepository.getLinks(chatId)
    } yield assert(links.find(_.url == "url1").exists(_.lastUpdate.contains(updateTime)))).unsafeRunSync()
  }

  it should "LinkRepository should update links and preserve tags" in withContainers { container =>
    val repositories = mkRepository(container)
    val chatId       = 3L
    val updateTime   = Instant.ofEpochMilli(1776365914822L)
    val tags1        = Set("scala", "functional")
    val tags2        = Set("java", "oop")

    val link1 = Link("url1", "api.url1", Stackoverflow, tags1, Set(chatId), None)
    val link2 = Link("url2", "api.url2", Stackoverflow, tags2, Set(chatId), None)

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)
      _ <- repositories.linkRepository.insertLink(chatId, link1)
      _ <- repositories.linkRepository.insertLink(chatId, link2)

      updatedLink1 = link1.copy(lastUpdate = Some(updateTime))
      _ <- repositories.linkRepository.updateLinks(List(updatedLink1))

      links <- repositories.linkRepository.getLinks(chatId)
      link1Result = links.find(_.url == "url1")
      link2Result = links.find(_.url == "url2")
    } yield {
      assert(link1Result.exists(_.lastUpdate.contains(updateTime)) &&
        link1Result.exists(_.tags == tags1) &&
        link2Result.exists(_.tags == tags2) &&
        link2Result.exists(_.lastUpdate.isEmpty))
    }).unsafeRunSync()
  }
}
