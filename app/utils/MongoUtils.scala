package utils

import com.mongodb.DBObject
import org.json4s.mongo.JObjectParser
import org.json4s.{DefaultFormats, Extraction}

trait MongoUtils {

  implicit val formats = new DefaultFormats {}

  protected def dbObjTo[A](from: DBObject)(implicit manifest: Manifest[A]): A = {
    val jValue = JObjectParser.serialize(from)
    val entity: A = Extraction.extract[A](jValue)
    entity
  }

  case class ID(_id: String)
  protected def toDBObj(any: Any): DBObject = {
    val json = Extraction.decompose(any)
//    val id = Extraction.extract[String](json \ "id")
//    val newJson = json merge (Extraction.decompose(ID(id)))
    val parsed = JObjectParser.parse(json)
    parsed
  }
}
