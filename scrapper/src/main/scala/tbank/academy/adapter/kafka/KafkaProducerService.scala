package tbank.academy.adapter.kafka

import cats.effect.{Async, Resource, Sync}
import org.apache.kafka.clients.producer.{KafkaProducer => JKafkaProducer}
import org.apache.kafka.clients.producer.ProducerConfig
import tbank.academy.config.{AppConfig, KafkaConfig}
import tbank.academy.kafka.KafkaMessage
import tofu.WithContext
import tethys._
import tethys.jackson._

import java.util.Properties

trait KafkaProducerService[F[_]] {
  def producePullRequest(message: KafkaMessage.PullRequestUpdate): F[Unit]
  def produceIssue(message: KafkaMessage.IssueUpdate): F[Unit]
  def produceComment(message: KafkaMessage.CommentUpdate): F[Unit]
  def produceAnswer(message: KafkaMessage.AnswerUpdate): F[Unit]
}

object KafkaProducerService {
  def make[F[_]: Async](kafkaConfig: KafkaConfig): Resource[F, KafkaProducerService[F]] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers.mkString(","))
    props.put(ProducerConfig.ACKS_CONFIG, kafkaConfig.producer.acks)
    props.put(ProducerConfig.RETRIES_CONFIG, Integer.valueOf(kafkaConfig.producer.retries))
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.valueOf(kafkaConfig.producer.batchSize))
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "link-tracker-scrapper")

    Resource
      .make(Sync[F].delay(new JKafkaProducer[String, String](
        props,
        new org.apache.kafka.common.serialization.StringSerializer,
        new org.apache.kafka.common.serialization.StringSerializer
      )))(p => Sync[F].delay(p.close()))
      .map { producer =>
        new KafkaProducerService[F] {
          private def send(topic: String, key: String, value: String): F[Unit] = {
            Sync[F].delay {
              producer.send(new org.apache.kafka.clients.producer.ProducerRecord[String, String](topic, key, value))
              ()
            }
          }

          override def producePullRequest(message: KafkaMessage.PullRequestUpdate): F[Unit] = {
            val json = message.asJson
            send(kafkaConfig.topics.pullRequest, message.url, json)
          }

          override def produceIssue(message: KafkaMessage.IssueUpdate): F[Unit] = {
            val json = message.asJson
            send(kafkaConfig.topics.issue, message.url, json)
          }

          override def produceComment(message: KafkaMessage.CommentUpdate): F[Unit] = {
            val json = message.asJson
            send(kafkaConfig.topics.comment, message.question, json)
          }

          override def produceAnswer(message: KafkaMessage.AnswerUpdate): F[Unit] = {
            val json = message.asJson
            send(kafkaConfig.topics.answer, message.question, json)
          }
        }
      }
  }

  def make[F[_]: Async](implicit context: WithContext[F, AppConfig]): Resource[F, KafkaProducerService[F]] =
    Resource.eval(context.ask(_.kafka)).flatMap(make[F])
}
