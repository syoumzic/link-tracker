package tbank.academy.adapter.persistance.slickDB

import io.scalaland.chimney.Transformer
import tbank.academy.{Github, Site, Stackoverflow}

object Transfromer {
  implicit val SiteT: Transformer[String, Site] = {
    case "github"        => Github
    case "stackoverflow" => Stackoverflow
  }
}
