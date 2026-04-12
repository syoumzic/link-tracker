package tbank.academy

import derevo.{Derevo, Derivation, NewTypeDerivation, delegating}
import tethys.JsonReader
import tethys.derivation.builder.{FieldStyle, ReaderDerivationConfig}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator

object Predef {
  @delegating(
    "tethys.derivation.semiauto.jsonReader",
    ReaderDerivationConfig.empty.withFieldStyle(FieldStyle.lowerSnakecase)
  )
  object tethysReaderSnake extends Derivation[JsonReader] with NewTypeDerivation[JsonReader] {
    def instance[A]: JsonReader[A] = macro Derevo.delegate[JsonReader, A]
  }

  implicit val unitReader: JsonReader[Unit] = new JsonReader[Unit] {
    override def read(it: TokenIterator)(implicit fieldName: FieldName): Unit = {
      it.nextToken()
      ()
    }
  }

  implicit def toInt(long: Long): Int = long.toInt
}
