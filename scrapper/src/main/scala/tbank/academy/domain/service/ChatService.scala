package tbank.academy.domain.service

import tbank.academy.domain.repository.ChatRepository

trait ChatService[F[_]] {
  def registerChat(chatId: Long): F[Unit]
  def deleteChat(chatId: Long): F[Unit]
}

object ChatService {
  def make[F[_]](chatRepository: ChatRepository[F]): ChatService[F] = new ChatService[F] {
    override def registerChat(chatId: Long): F[Unit] = chatRepository.registerChat(chatId)

    override def deleteChat(chatId: Long): F[Unit] = chatRepository.deleteChat(chatId)
  }
}
