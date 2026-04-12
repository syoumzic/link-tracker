package tbank.academy.adapter.persistance.slickDB

import slick.jdbc.PostgresProfile.api._
import java.time.Instant
import slick.lifted.ProvenShape

object Domain {
  class Chats(tag: Tag) extends Table[Long](tag, "chats") {
    def chatId: Rep[Long] = column[Long]("chatid", O.PrimaryKey)

    def * = chatId
  }
  val chats: TableQuery[Chats] = TableQuery[Chats]

  case class LinkRow(id: Long, chatId: Long, url: String, apiUrl: String, site: String, lastUpdate: Option[Instant])

  class Links(tag: Tag) extends Table[LinkRow](tag, "links") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def chatId: Rep[Long] = column[Long]("chatid")

    def url: Rep[String] = column[String]("url")

    def apiUrl: Rep[String] = column[String]("apiurl")

    def site: Rep[String] = column[String]("site")

    def lastUpdate: Rep[Option[Instant]] = column[Option[Instant]]("lastupdate")

    def * : ProvenShape[LinkRow] = (id, chatId, url, apiUrl, site, lastUpdate).mapTo[LinkRow]
  }
  val links: TableQuery[Links] = TableQuery[Links]

  class Tags(tag: Tag) extends Table[(Long, String)](tag, "tags") {
    def linkId: Rep[Long] = column[Long]("linkid")

    def name: Rep[String] = column[String]("name")

    def * : ProvenShape[(Long, String)] = (linkId, name)
  }
  val tags: TableQuery[Tags] = TableQuery[Tags]
}
