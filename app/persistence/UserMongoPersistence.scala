package persistence

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, UUID}

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import com.mongodb.casbah.{MongoClient, MongoDB}
import com.mongodb.casbah.commons.MongoDBObject
import controllers._
import org.joda.time.DateTime
import org.json4s.mongo.JObjectParser
import org.json4s.{DefaultFormats, Extraction}
import persistence.EnrichedPayment.MDB
import persistence.MongoObject.client
import persistence.TableName.users
import utils.MongoUtils
import org.json4s.native.JsonMethods._


object MongoObject {
  lazy val client = MongoClient()

}
object DBName {
  val publish = "publish"
}
object TableName {
  val user_mizvas = "user_mizvas"
  val obligations = "obligations"
  val mizvas = "mizvas"
  val users = "users"
  val clicks = "clicks"
  val chat = "chat"
  val usertoken = "usertoken"
  val pcampaign = "pcampaign"
  val payviews = "payviews"
  val userviews = "userviews"
  val publishclicks = "publishclicks"
  val payments = "payments"
  val connections = "connections"
  val posts = "posts"
  val campaigns = "campaigns"
  val campaigntouser = "campaigntouser"
  val notifications = "notifications"
  val earnings = "earnings"
  val endorsement = "endorsement"
  val reviews = "reviews"
}

case class Messages(_id : String, from:Option[String] = None, to: Option[String] = None,lastupdate : Long, messages : List[String])
case class MessagesEnriched(id : String, from:Option[String] = None,
                            fromName : Option[String],fromImage : Option[String],
                            to: Option[String] = None,
                            toName : Option[String], toImage : Option[String],
                            lastupdate : Long, lastupdateTimestamp : Long, messages : List[String])

object MessagesEnriched {
  def fromMassages(m : Messages, p : UserPostgresPersistence): MessagesEnriched = {
    val fd = m.from.map(x=> p.getUser(x)).flatten
    val td = m.to.map(x=> p.getUser(x)).flatten

    MessagesEnriched(m._id,m.from,fd.map(x=>x.name +  " " + x.lastname.getOrElse("")),fd.map(x=>x.image).flatten,
      m.to,td.map(x=>x.name + " " + x.lastname.getOrElse("")),td.map(x=>x.image).flatten,

      System.currentTimeMillis() -  m.lastupdate, m.lastupdate,m.messages)
  }
}
case class DashboardDetails(referrals : Integer, referral_views : Integer, payments : Integer, pay_views : Integer, chart : List[NameSeries], earnings : Option[Double]  = None, deals : Option[Integer] = None )
case class Earning(_id: String, username : String, amount : Double, message  : String, time : Long, user2 : String)

case class Provider(profession : Option[String] = None, category : Option[String]=None,services : Option[String] = None, website : Option[String]= None,
                    about : Option[String] = None, endorsments : List[EnrichedEndorsement] = List.empty[EnrichedEndorsement])
case class ProviderUI(profession : Option[String] = None, category : Option[String]=None,services : Option[String] = None, website : Option[String]= None, about : Option[String] = None, address : Option[String] = None ,
                      longitude : Option[Double] = None, latitude : Option[Double] = None) {
  def toProvider = Provider(this.profession,this.category,this.services,this.website,this.about)
}
case class PublishAgent(sex : String, birthday : Long, family_status: String, no_children : Integer, household_income : String)
case class PublishAgentUI(sex : String, birthday : Long, family_status: String, no_children : Integer, household_income : String,address : Option[String] = None ,
                          longitude : Option[Double] = None, latitude : Option[Double] = None) {
  def toPublishAgent = PublishAgent(this.sex,this.birthday,this.family_status,this.no_children,this.household_income)
}
case class PublishResponse(user : String, provider : String, referral : Option[String], rate : Option[Int] = None, title : Option[String]= None, review : Option[String]= None)
case class PublishCampaign(_id : String, provider : String, offer_date: Long, valid_until: Option[Long], title : String, description : String, image : String, users : List[String] = List.empty[String], responds : List[PublishResponse])
case class User(_id: String, name : String, lastname : Option[String] = None,password : Option[String],phone : Option[String] = None, referral : Option[String],
                providing : Option[Provider] = None, publishagent: Option[PublishAgent] = None, email : Option[String]= None,address : Option[String]= None,balance : Option[Integer] = None,image: Option[String] = None,lastseen : Option[Long] = None,
                longitude: Option[Double] = None,latitude : Option[Double] = None,
                reffered_users :List[EnrichedClick]  = List.empty[EnrichedClick],
                reffered_users_views :List[EnrichedClick]  = List.empty[EnrichedClick],
                reffered_payments_views :List[EnrichedClick]  = List.empty[EnrichedClick],
                provider_views :List[EnrichedClick]  = List.empty[EnrichedClick],
                pay_payed : List[EnrichedPayment]  = List.empty[EnrichedPayment],
                pay_received : List[EnrichedPayment]  = List.empty[EnrichedPayment],
                pay_referral : List[EnrichedPayment]  = List.empty[EnrichedPayment],
                publish_clicks : List[EnrichedClick] = List.empty[EnrichedClick],
                notifications: List[String] = List.empty[String],
                notifications_new : List[NotificationNew] = List.empty[NotificationNew],
                lastSeenNotification : Option[Int] = None,
                gallery: List[String] = List.empty[String],
                portfolio: List[PortfolioItem] = List.empty[PortfolioItem],
                stats : Option[ProviderStats] = None,
                agentstats : Option[AgentStats] = None,
                messages: Option[InviteMessages] = None,
                withdrawAccounts : Option[Map[String, UserWithdrawAccount]] = None,
                earnings : List[Earning] = List.empty[Earning],
                badge : Option[Integer] = Some(1),
                agent_reviews : List[EnrichedReview] = List.empty[EnrichedReview],
                notification_id : Option[String] = None
               ){
  def fullName = this.name + " " + this.lastname.getOrElse("")
  def forMigration ={
    this.copy(
      reffered_users = reffered_users.map(x=>x.forMigration),
      reffered_users_views  = reffered_users_views.map(x=>x.forMigration),
      reffered_payments_views= List.empty[EnrichedClick],// reffered_payments_views.map(x=>x.forMigration).filter(x=>x.timestamp > 1522026499153L),
      provider_views =provider_views.map(x=>x.forMigration).sortBy(x=>x.timestamp),
      pay_payed =pay_payed.map(x=>x.forMigration),
      pay_received= pay_received.map(x=>x.forMigration),
      pay_referral= pay_referral.map(x=>x.forMigration),
      publish_clicks= publish_clicks.map(x=>x.forMigration.copy(source = "")).sortBy(x=>x.timestamp))
  }

}
case class UserConnection( phone : String, full_name : String,connection_type : Option[String] = None, user_image : Option[String] = None ,  profit : Option[Double] = Some(0.0))
object User {

}
case class UserWithdrawAccount(firstName : String, lastName : String, email : String, phone : String, extraInfo : String, verified : Boolean)
case class NameImage(name : String, image : String)
case class ProviderStats(distance : Double, reviews : Integer, avg_score : Double, grouping : String)
case class AgentStats(distance : Double, reviews : Integer, deals : Integer, grouping : String)
case class Payment(payer : Option[String], provider : String, referral : Option[String], amount : Double, rate : Option[Int] = None, title : Option[String]= None, review : Option[String]= None, stripe_token: String)
case class PGPayment(payer : Option[String], provider : String, referral : Option[String], amount : Double, rate : Option[Int] = None, title : Option[String]= None, review : Option[String]= None, stripe_token: String, _id : String, timestamp: Long)
case class ReviewUI(user : String, rate : Int,  review : Option[String]= None)
case class Review(provider : String, user : String, rate : Int,  review : Option[String]= None,_id : String, timestamp: Long)
case class EnrichedReview(provider : String, user : String, rate : Int,  review : Option[String]= None,_id : String, timestamp: Long, timestamp_str : String, provider_str : String, provider_img : String)
case class Click(source: String, target : String, referral : String, timestamp: Option[Long] = None, _id : Option[String] = None)
case class Endorsement(user: String, provider : String, service : String, timestamp: Long, _id : String)
case class EndorsementUI(provider: String, service : String)
case class EnrichedEndorsement(user: String, provider : String, id : String, timestamp: Long, timestamp_str : String, user_str : String, user_img : String, service : String){
  def forTest = {
    this.copy(id = "",timestamp=0L,timestamp_str="")
  }

}
case class EnrichedClick(source: String, target : String, referral : String,
                         id : String, timestamp: Long, timestamp_str : String, source_str : String, target_str : String, referral_str : String,
                         source_img : String, target_img : String, referral_img : String,
                         target_services : Option[String] = None, source_badge : Option[Integer] = None, target_badge : Option[Integer] = None, referral_badge : Option[Integer] = None,
                         target_rating : Option[Double] = None, target_reviews : Option[Integer] = None
                        ){
  def forTest = {
    this.copy(id = "",timestamp=0L,timestamp_str="")
  }
  def forMigration ={
    this.copy(source_str = "",target_str="",referral_str="", source_img = "", target_img= "", referral_img= "")
  }
}
case class TData(_id : String, user : String)
case class EnrichedPayment(source : String, target : String, referral : String, amount : Double, rate : Option[Int] = None, title : Option[String]= None, review : Option[String]= None,
                           payment_id : String, timestamp: Long, timestamp_str : String, source_str : String, target_str : String, referral_str : String,
                           source_img : String, target_img : String, referral_img : String,
                           target_services : Option[String] = None, source_badge : Option[Integer] = None, target_badge : Option[Integer] = None, referral_badge : Option[Integer] = None
                          ){
  def forMigration ={
    this.copy(source_str = "",target_str="",referral_str="", source_img = "", target_img= "", referral_img= "")
  }
}
case class UserToken(username : String, token : String)
case class SendOTP(phone : Option[String], calltype : String, user : String )
case class VerifyOTP(calltype : String, user : String )
case class GoogleToken(email : String, token : String)

object PGPayment{
  def fromPayment(c :Payment)= {
    PGPayment(c.payer,c.provider,c.referral,c.amount,c.rate,c.title,c.review,c.stripe_token,UUID.randomUUID().toString,System.currentTimeMillis())
  }
}

object EnrichedPayment{
    def MDB = MongoDBObject
  def fromPayment(c :Payment, db : MongoDB)= {
      val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
      val s = db(users).findOne(MDB("_id"->c.payer),MDB("_id"->0,"name"->1,"lastname" ->1, "image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String] + " " + x.get("lastname").asInstanceOf[String],x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
      val t = db(users).findOne(MDB("_id"->c.provider),MDB("_id"->0,"name"->1,"lastname" ->1, "image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String] + " " + x.get("lastname").asInstanceOf[String],x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
      val r = db(users).findOne(MDB("_id"->c.referral),MDB("_id"->0,"name"->1, "lastname" ->1,"image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String] + " " + x.get("lastname").asInstanceOf[String] ,x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
      val id = UUID.randomUUID().toString;
      EnrichedPayment(c.payer.getOrElse(""), c.provider, c.referral.getOrElse(""), c.amount, c.rate, c.title, c.review,
        id, System.currentTimeMillis(), minuteFormat.format(Calendar.getInstance().getTime()),
        s.name, t.name, r.name,s.image,t.image,r.image)

  }
}

class UserMongoPersistence(dbName: String) extends AppPersistance with  MongoUtils {

  import TableName._
  lazy val db = client.getDB(dbName)

  def MDB = MongoDBObject

  def enrichedClick_fromClick(c :Click)= {
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
    val s = db(users).findOne(MDB("_id"->c.source),MDB("_id"->0,"name"->1, "lastname" ->1,"image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String]+ " " + x.get("lastname").asInstanceOf[String],x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
    val t = db(users).findOne(MDB("_id"->c.target),MDB("_id"->0,"name"->1, "lastname" ->1,"image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String]+ " " + x.get("lastname").asInstanceOf[String],x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
    val r = db(users).findOne(MDB("_id"->c.referral),MDB("_id"->0,"name"->1, "lastname" ->1,"image" ->1)).map(x=> NameImage( x.get("name").asInstanceOf[String]+ " " + x.get("lastname").asInstanceOf[String],x.get("image").asInstanceOf[String])).getOrElse(NameImage("",""))
    val id = UUID.randomUUID().toString;
    EnrichedClick(c.source, c.target, c.referral ,
      id, System.currentTimeMillis(), minuteFormat.format(Calendar.getInstance().getTime()),
      s.name, t.name, r.name,s.image,t.image,r.image)

  }

}
//
//case class TST(a: String, m : Map[String,String] = Map.empty[String,String])
//object test extends App with  MongoUtils {
//  def details(on : Int) = {
//    MDB("pay_referral" -> on, "pay_received" -> on, "pay_payed" -> on,  "provider_views" -> on,
//      "reffered_payments_views" -> on, "reffered_users_views"-> on, "reffered_users"->on,"referral"->on,"publish_clicks" ->on,
//      "messages" -> on,"notifications" ->on, "withdrawAccounts" -> on
//    )
//  }
//  val str = """{"a":"2"}"""
//  println(Extraction.extract[TST](parse(str)))
//  val condition = List(
//     Some("providing.profession" -> "ccc"),
//    Some("providing" -> MDB("$exists" -> 1)))
//  val searchRule = MDB(condition.flatten)
//  println(searchRule)
//
//    def MDB = MongoDBObject
//  lazy val db = client.getDB("publish")
//  //db("test").insert(toDBObj(User("ccccc","mmmm",referral=None, password = "ccc",withdrawAccounts = Map("paypal"->UserWithdrawAccount("hh","ff","sss","ssss","ssss")))))
//  val x= db("users").find(searchRule,details(0)).map(y=>JObjectParser.serialize(y)).toList
//  println(x)
//  println(x.map(f=>Extraction.extract[User](f)))
////  db("test").update(MDB("_id"->"ccccc"),MDB("$set" -> MDB("withdrawAccounts.gmail" -> toDBObj(UserWithdrawAccount("hh1","ff","sss","ssss","ssss")))))
//  println("after")
//
//}