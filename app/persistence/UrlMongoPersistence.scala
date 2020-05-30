package persistence

import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import persistence.MongoObject.client
import utils.MongoUtils

import scala.util.Try

object TableNameUrls {
  val urls = "urls"
}

case class MongoUrl(_id: String)
case class MongoBookmark(_id: String, url: String, alias: String, clicks: Int, user: Option[String] = None)

class UrlMongoPersistence(dbName: String) extends MongoUtils {

  import TableNameUrls._

  lazy val db = client.getDB(dbName)

  def MDB = MongoDBObject
  def addMiniUrlId(url: MongoUrl) = {
    db(urls).insert(toDBObj(url))
  }
  def TakeMiniUrlById(miniurl: String, bookmark: Bookmark) = {
    db(urls).update(MDB("_id" -> miniurl), MDB("$set" -> bookmark.getAsMongoFields()))
  }

  def getUrlByMini(miniurl: String, incClicks: Boolean = false): Option[Bookmark] = {
    val res = db(urls).findOne(MDB("_id" -> miniurl), MDB("_id" -> 0)).map(x => dbObjTo[Bookmark](x))
    if (incClicks) {
      db(urls).update(MDB("_id" -> miniurl), MDB("$inc" -> MDB("clicks" -> 1)))
    }
    res
  }
  def getUrlByUser(user: String) = {
    db(urls).find(MDB("user" -> user)).map(x => dbObjTo[MongoBookmark](x)).toList
  }

  def getUsedUrls(user: Option[String]): List[MongoBookmark] = {
    val filter = List("url" -> MDB("$exists" -> true)) ++ user.map(x => "user" -> x).toList
    db(urls).find(MDB(filter)).sort(MDB("timestamp" -> -1)).map(x => dbObjTo[MongoBookmark](x)).toList
  }
  def deleteMiniUrl(miniurl: String) = {
    db(urls).remove(MDB("_id" -> miniurl))
  }
  def updateAlias(miniurl: String, alias: String) = {
    db(urls).update(MDB("_id" -> miniurl), MDB("$set" -> MDB("alias" -> alias)))
  }

  def createIndexes() = {
    db(urls).createIndex(MDB("url" -> 1, "user" -> 2))
  }

}

case class MiniUrlGen(db: UrlMongoPersistence, length: Integer = 6) {
  val allowed = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
  val r = scala.util.Random
  var totalTimeInside = 0L
  def genUrl(): String = {

    tryXTimes({
      val key = (1 to length).map(x => allowed(r.nextInt(allowed.size))).mkString
      val t = System.currentTimeMillis();
      db.addMiniUrlId(MongoUrl(key))
      totalTimeInside += (System.currentTimeMillis() - t)
      key
    }, times = 10)

  }

  def tryXTimes(f: => String, times: Integer): String = {
    if (times == 0) throw new RuntimeException("Unable to create a key")
    Try { f }.recover {
      case e: Throwable =>
        tryXTimes(f, times - 1)
    }.get
  }
}

case class Bookmark(url: String, alias: String, clicks: Integer = 0, user: Option[String] = None) {
  def getUrl() = {
    if (url.startsWith("http")) {
      url
    } else {
      "http://" + url
    }
  }

  def getAsMongoFields(): DBObject = {
    val fields = List("url" -> url, "alias" -> alias, "clicks" -> clicks, "timestamp" -> System.currentTimeMillis())
    MongoDBObject(fields ++ user.map(x => ("user" -> x)).toList)
  }

}