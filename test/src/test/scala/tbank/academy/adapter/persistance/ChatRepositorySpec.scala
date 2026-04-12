package tbank.academy.adapter.persistance

import cats.effect.unsafe.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait ChatRepositorySpec extends AnyFlatSpec with Matchers with RepositorySpec {
  behavior of "ChatRepository"

  it should "create and delete chat" in withContainers { container =>
    val repositories = mkRepository(container)

    val chatId = 1L

    (for {
      _ <- repositories.chatRepository.registerChat(chatId)
      _ <- repositories.chatRepository.deleteChat(chatId)
    } yield ()).unsafeRunSync()
  }
}
