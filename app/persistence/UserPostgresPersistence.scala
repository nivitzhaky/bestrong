package persistence

import java.sql.{Connection, Statement}
import java.text.SimpleDateFormat
import java.{lang, util}
import java.util.{Calendar, Date, Properties, UUID}

import com.firebase4s.App
import com.firebase4s.database.{Database, DatabaseReference}
import com.google.api.client.util.StringUtils
import com.mongodb.casbah.MongoClient
import controllers._
import org.json4s.Extraction
import persistence.TableName._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.mongo.JObjectParser
import play.shaded.ahc.io.netty.util.internal.StringUtil

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try

case class QuickSearchResult(full_name : String, phone : String, user_type :  String, username : Option[String]= None, target_user : Option[String] = None, target_image : Option[String] = None)
case class UserPost(_id : String, user : String, image : Option[String], post_text :  String, post_time : Long)
case class UIPost(user : String, image : Option[String], post_text :  String)
case class Campaign(_id : String,provider : String,  title : String , description : String, startTime : Long, endTime : Option[Long], image : Option[String])
case class CampaignDetailedStats(invited : List[NameImage], accepted : List[NameImage], reviewed : List[NameImage])
case class CampaignStats(invited : Integer, accepted : Integer, reviewed : Integer)
case class CampaignWithStats(campaign: Campaign, stats : CampaignStats)
case class CampaignWithDetailedStats(campaign: Campaign, stats : CampaignDetailedStats)
case class CampaignUI(title : String , description : String, startTime : Long, endTime : Option[Long], image : String)
case class InviteCampaignUI(campaign_id: String , usernames :  List[String])
case class AcceptCampaignUI(campaign_id: String , username :  String)
case class NotificationNew(_id : String, username : String, notification_user : Option[String], message : String, time : Long)
case class Mizva(_id : String, name : String, reminderHour : Option[String], order : Integer)
case class ExtraInfo(days: Option[String], duration : Option[Integer], notes : Option[String] )
case class MizvaExtraInfo(duration : Option[Integer], notes : Option[String] )
case class ObligationUI(mizva : String, user : String, reminderHour : Option[String], extraInfo : Option[ExtraInfo])
case class Obligation(_id : String, mizva : String, user: String, reminderHour : Option[String], extraInfo: Option[ExtraInfo])
case class UserMizvaUI(mizva : String,  extraInfo : Option[MizvaExtraInfo])


object PostgresObject {
  lazy val client = MongoClient()

  import java.sql.Connection
  import java.sql.DriverManager

  val url = "jdbc:postgresql://localhost:5432/postgres?currentSchema=myschema"
  val cons = TrieMap.empty[String, Connection]
  def getConn(db : String, user: String, password : String, schema : String, host: String = "localhost", port : String = "5432" ): Connection = {
    val props = new Properties()
    props.setProperty("user", user)
    props.setProperty("password", password)
    val theUrl = url.replace("5432/postgres","5432/" + db).replace("myschema",schema).replace("localhost:",host+ ":").replace("5432",port)
     cons.getOrElseUpdate(port + schema,  DriverManager.getConnection(theUrl, props) )
  }
}


class UserPostgresPersistence (db1 : String, user: String, password : String, schema : String, db : Option[Database] = None, host: String = "localhost", port : String = "5432")   extends PostgresUtils{
  val fooRef = db.map(x=>  x.ref("notifications"))

  def addObligation(o: ObligationUI) = {
    val obligation = Obligation(UUID.randomUUID().toString,o.mizva,o.user,o.reminderHour,o.extraInfo)
    db(obligations).insertObligation(obligation)
    obligation
  }

  def getObligations(user : String) = {

    getRST[Obligation](
      s"""select data from $obligations
         |where username = '$user'
         |""".stripMargin)

  }

  def addMizva(m: Mizva) = {
    db(mizvas).insert[Mizva](m)
    m
  }

  def addUserMizva(user: String,  m: UserMizvaUI) = {
    db(user_mizvas).insertUserMizva( user ,m)
    m
  }
  def getMizvas() = {
    getRST[Mizva](
      s"""select data from $mizvas
         | order by (data->>'order')::bigint desc""".stripMargin)

  }


  def cs = gconn.createStatement()
  implicit val formats1 = DefaultFormats

  val conn = PostgresObject.getConn(db1, user, password, schema, host,port)

  if (schema.equals("postgres")) {
    cs.execute("""alter user postgres set search_path = 'public'""")
  }
  createTables()

//
//  def getCampaigns(provider : String) = {
//
//    val sql =
//      s"""
//         |select c.data::text,
//         | (select count(*) from $campaigntouser where campaign_id = c.id) num_invited,
//         | (select count(*) from $campaigntouser where campaign_id = c.id and accepted = true) num_accepted,
//         | (select count(*) from $campaigntouser ctu where campaign_id = c.id and exists(select 1 from payments where data->>'payer'::varchar =ctu.username and data->>'provider'::varchar = ctu.provider )) num_reviewed
//         | from $campaigns c
//         | where  c.data->>'provider' = '$provider'
//      """.stripMargin
//    val rs = cs.executeQuery(sql)
//    var res = ArrayBuffer.empty[CampaignWithStats]
//    while (rs.next()) {
//      res+=CampaignWithStats(
//        campaign = parse( rs.getString("data")).extract[Campaign],
//        stats = CampaignStats(
//          invited = rs.getInt("num_invited"),
//          accepted = rs.getInt("num_accepted"),
//          reviewed =  rs.getInt("num_reviewed"),
//        )
//      )
//    }
//    rs.close()
//    res.toList
//
//  }







//  def getSocialFeed(user : String) = {
//
//        getRST[UserPost](
//          s"""select distinct data::text,(data->>'post_time')::bigint  from (select data from $posts where data->>'user'::text = '$user'
//             | union all
//             | select data from $posts p
//             | where exists (select  1 from $connections where user2=(p.data->>'user')::text and username = '$user' )) as foo
//             | order by (data->>'post_time')::bigint desc""".stripMargin)
//
//  }


//  def getNotifications(user : String) = {
//
//    getRST[NotificationNew](
//      s"""select data from $notifications
//         | where data->>'username'::varchar = '$user'
//         | order by (data->>'time')::bigint desc""".stripMargin)
//
//  }
//



  def gconn = conn

  def createIndexes() = {
    val s = gconn.createStatement()
    Try{
      s.execute(
        s"""
           |CREATE  INDEX ${users}text ON $users
           |   USING gin ( to_tsvector('english',data) );
      """.stripMargin)}

    Try{s.execute( s"""CREATE INDEX ${users}email ON $users(cast("data"->>'email' AS varchar));""")}
    Try{s.execute( s"""CREATE INDEX ${users}phone ON $users(cast("data"->>'phone' AS varchar));""")}
    Try{s.execute( s"""CREATE INDEX ${posts}user ON $users(cast("data"->>'user' AS varchar));""")}
    Try{s.execute( s"""CREATE UNIQUE INDEX ${connections}userphone ON $connections(username, phone);""")}
    Try{s.execute( s"""CREATE UNIQUE INDEX ${connections}useruser2 ON $connections(username, user2);""")}
    Try{s.execute( s"""CREATE unique INDEX ${campaigntouser}provideruserid ON $campaigntouser(provider, username, campaign_id);""")}
    Try{s.execute( s"""CREATE INDEX ${notifications}user ON $notifications(cast("data"->>'username' AS varchar));""")}
    Try{s.execute( s"""CREATE INDEX ${earnings}user ON $earnings(cast("data"->>'username' AS varchar));""")}
    Try{s.execute( s"""CREATE INDEX ${endorsement}provider ON $endorsement(cast("data"->>'provider' AS varchar));""")}
    Try{s.execute( s"""CREATE INDEX ${reviews}user ON $reviews(cast("data"->>'user' AS varchar));""")}
  }
  def createTables(): Unit = {
    val s = gconn.createStatement()
//    if (user == "test")
//      s.execute("""alter user test set search_path = 'test'""");

    s.execute(s"""CREATE TABLE IF NOT EXISTS $user_mizvas (username varchar(50), mizva varchar(50),  duration int, quality int, notes varchar(400), created timestamp );""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $obligations (id varchar CONSTRAINT ${obligations}key PRIMARY KEY, username varchar(50), mizva varchar(50),  data  json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $mizvas (id varchar CONSTRAINT ${mizvas}key PRIMARY KEY, data  json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $users (id varchar CONSTRAINT ${users}key PRIMARY KEY, data  json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $usertoken (id varchar CONSTRAINT ${usertoken}key PRIMARY KEY,  data  json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $chat (id varchar CONSTRAINT ${chat}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $clicks (id varchar CONSTRAINT ${clicks}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $payviews (id varchar CONSTRAINT ${payviews}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $userviews (id varchar CONSTRAINT ${userviews}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $publishclicks (id varchar CONSTRAINT ${publishclicks}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $payments (id varchar CONSTRAINT ${payments}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $posts (id varchar CONSTRAINT ${posts}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $connections (username varchar, phone varchar, full_name varchar, user2 varchar);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $campaigns (id varchar CONSTRAINT ${campaigns}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $campaigntouser (campaign_id varchar, provider varchar, username varchar, accepted BOOLEAN );""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $notifications (id varchar CONSTRAINT ${notifications}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $earnings (id varchar CONSTRAINT ${earnings}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $endorsement (id varchar CONSTRAINT ${endorsement}key PRIMARY KEY, data json);""".stripMargin)
    s.execute(s"""CREATE TABLE IF NOT EXISTS $reviews (id varchar CONSTRAINT ${reviews}key PRIMARY KEY, data json);""".stripMargin)
    createIndexes()
  }





  def getAgentReviews(name: String): scala.List[EnrichedReview] = {
      val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
      val sql =
        s"""
           |select rate, review, provider, _id, timestamp, COALESCE(provider_full_name,'') as provider_full_name, COALESCE(provider_img,'') as provider_img
           |from (
           |select p.data ->>'rate' as rate, p.data->>'review' as review, p.data->>'provider' as provider,p.data->>'_id' as _id, p.data->>'timestamp' as timestamp,
           | usrc.data->>'image' as provider_img  ,(cast(usrc.data->>'name' as varchar) || ' ' || COALESCE(cast(usrc.data->>'lastname' as varchar),'' :: varchar))  as provider_full_name
           | from $reviews p
           | LEFT JOIN users usrc ON (usrc.data->>'_id' = p.data->>'provider')
           | where p.data->>'user' = '$name'
           | ) as foo order by timestamp
      """.stripMargin
      val rs = gconn.createStatement().executeQuery(sql)
      var res = ArrayBuffer.empty[EnrichedReview]
      while (rs.next()) {
        val d  = new Date(rs.getLong("timestamp"));
        res+=EnrichedReview(
          rate = rs.getInt("rate"),
          review = Option(rs.getString("review")),
          user = name,
          provider = rs.getString("provider"),
          provider_str = rs.getString("provider_full_name"),
          _id = rs.getString("_id"),
          timestamp = rs.getLong("timestamp"),
          timestamp_str = minuteFormat.format(d),
          provider_img =  rs.getString("provider_img")
        )
      }
      rs.close()
      return res.toList

    }

    def getUser(name : String, queryStats : Boolean = false): Option[User] = {
    val a =  db(users).findOne[User]( name)
    if (!(queryStats)) {
      return a
    }
    val withStats = a.map(x =>x.copy(stats = Some(ProviderStats(0.0,0,0,""))))
    val withRef = withStats.map(y=> y.copy(reffered_users =   getEnriched(s"$clicks.data->>'source' = '$name'",clicks)))
    val withPay = withRef.map(y=>
      y.copy(provider_views =   getEnriched(s"$payviews.data->>'target' = '$name'",payviews),
        reffered_payments_views =   getEnriched(s"$payviews.data->>'referral' = '$name'",payviews),
        reffered_users_views = getEnriched(s"$userviews.data->>'referral' = '$name'",userviews),
        publish_clicks = getEnriched(s"$publishclicks.data->>'source' = '$name'",publishclicks),
        pay_received = getEnrichedPayment(s"p.data->>'provider' = '$name'"),
        pay_referral = getEnrichedPayment(s"p.data->>'referral' = '$name'"),
        pay_payed = getEnrichedPayment(s"p.data->>'payer' = '$name'"),
        agent_reviews = getAgentReviews(name)
      ))
    withPay.map{x=>
      val pays = x.pay_received.size
      val reviews  = x.pay_received.count(p=>(p.review.getOrElse("") !=""))
      val scorel: Seq[Int] = x.pay_received.map(p=>p.rate).flatten
      val s = (if (scorel.size==0) 0.0 else (scorel.foldRight(0)( _ + _)).toDouble/ (scorel.size).toDouble)
      val score = (math floor s * 100) / 100
      x.copy(stats = Some(ProviderStats(VERY_FAR,reviews,score,"")))

    }

  }

  def getUserForEmail(email: String) : Option[String] = {
    getGetOne(s"select id from $users where data->>'email' = '$email'")
  }

  def getUserForPhone(phone: String) : Option[String] = {
    getGetOne(s"select id from $users where data->>'phone' = '$phone'")
  }

  def addUser(user: User ) : String= {

    println("adding:" + user)
    db(users).insert(user)
    user.referral.map(ref=> db(clicks).insert(Click(ref,user._id,"", _id = Some(UUID.randomUUID().toString),timestamp = Some(System.currentTimeMillis()))))
    user.phone.map(p=>
      executeUpdate(s"""update $connections set user2 = '${user._id}' where phone = '$p' """)
    )
    //    val referral_rec = enrichedClick_fromClick(Click(user.referral.getOrElse(""),user._id,""))
    //    db(users).update(MDB("_id"->user.referral),
    //      MDB("$push"->MDB("reffered_users"->toDBObj(referral_rec))))


    // TODO: check about num reffered users
    //    db(users).update(MDB("_id"->user.referral),MDB("$inc"->MDB("num_reffered_users" -> 1)))
    generateToken(user._id)
  }



  def getUserForToken(t: String) : Option[String] = {
    val curtime = System.currentTimeMillis()
    val u =  db(usertoken).findOne[TData](t).map(x=>x.user)
    u.foreach(usr => db(users).updateProp(usr, s"""{"lastseen" : $curtime}"""))
    u

  }

  def generateToken(user: String) : String = {
    val token = UUID.randomUUID().toString
    db(usertoken).insert(TData(token,user))
    token
  }
//
//  def incPayViews(views: IncPayViews) = {
//    val referral_rec = Click(views.payer.getOrElse(""), views.provider, views.referral.getOrElse(""),Some(System.currentTimeMillis()),Some(UUID.randomUUID().toString))
//    Option(views.provider).foreach { x =>
//      db(payviews).insert(referral_rec)
//    }
//
//  }
//
//  def incUserViews(data: IncUserViews) = {
//    val referral_rec = Click("", "", data.referral, Some(System.currentTimeMillis()),Some(UUID.randomUUID().toString))
//    println("provider was empty incing")
//    db(userviews).insert(referral_rec)
//  }
//  def publishClick(user : String, provider : String ) = {
//    val referral_rec = Click(user, provider, "", Some(System.currentTimeMillis()),Some(UUID.randomUUID().toString))
//    println("provider was empty incing")
//    db(publishclicks).insert( referral_rec)
//  }
//
//  def notificationClick(user : String, curSize : Int) = {
//    db(users).updateProp(user, s"""{"lastSeenNotification" : $curSize}""")
//  }

  def updateUserImage(id: String, url : String) = {
    println("before update " + id)
    db(users).updateProp(id,s"""{"image" : "$url"}""")
    println("after update " + id)
  }
//
//  def updateLocation(id: String, address : Option[String],longitude : Option[Double], latitude : Option[Double]) = {
//     address.foreach(x=> db(users).updateProp(id,s"""{"address" : "$x"}"""))
//    longitude.foreach(x=> db(users).updateProp(id,s"""{"longitude" : $x}"""))
//    latitude.foreach(x=> db(users).updateProp(id,s"""{"latitude" : $x}"""))
//  }
//
  def updatePassword(id: String, password: String) = {
    println("before update " + id)
    db(users).updateProp(id,s"""{"password" : "$password"}""")
    println("after update " + id)
  }

//  def updateUserMessages(id: String, messages : InviteMessages) = {
//    println("before update " + id)
//    val j = Extraction.decompose(messages)
//    db(users).updateProp(id,s"""{"messages" : ${compact(render(j))} }""");
//    println("after update " + id)
//
//  }
//
//  def updateUserGallery(id: String, urls : List[String]) = {
//    println("before update " + id)
//    val j = Extraction.decompose(urls)
//    db(users).updateProp(id,s"""{"gallery" : ${compact(render(j))} }""");
//    println("after update " + id)
//  }
//
//  def addPortfolioItem(id: String, item : PortfolioItem) = {
//    println("before add portfolio" + id)
//    db(users).push[PortfolioItem](id,"portfolio", item);
//  }
//
  def updateUser(name : String, update : UserUpdate) = {
    db(users).updateProp(name,s"""{"name": "${update.name}", "lastname": "${update.lastname}","address":"${update.address.getOrElse("")}", "phone":"${update.phone.getOrElse("")}","email":"${update.email.getOrElse("")}"} """)
  }
//
//  def getGrouping(distance: Double, reviews: Int, rating: Double, badge: Int, orderBy :Option[String], deals : Int = -1): String = {
//    orderBy match {
//      case Some("reviews") =>
//        if (reviews >= 200) "200+"
//        else if (reviews >= 100) "100+"
//        else if (reviews >= 50) "50+"
//        else if (reviews >= 20) "20+"
//        else if (reviews >= 10) "10+"
//        else "below 10"
//      case Some("deals") =>
//        if (deals >= 200) "200+"
//        else if (deals >= 100) "100+"
//        else if (deals >= 50) "50+"
//        else if (deals >= 20) "20+"
//        else if (deals >= 10) "10+"
//        else "below 10"
//      case Some("distance") =>
//        if (distance >= 200.0) "200 km+"
//        else if (distance >= 100) "100 km+"
//        else if (distance >= 50) "50 km+"
//        else if (distance >= 20) "20 km+"
//        else if (distance >= 10) "10 km+"
//        else "near you"
//      case Some("badge") =>
//        if (badge >= 4) "Platinum member"
//        else if (badge >= 3) "Gold member"
//        else if (badge >= 2) "Silver member"
//        else "Bronze member"
//      case _ =>
//        if (rating >= 4.5) "4.5* +"
//        else if (rating >= 4) "4* +"
//        else if (rating >= 3.5) "3.5* +"
//        else if (rating >= 3) "3* +"
//        else "below 3"
//    }
//  }
//
//  def getProviders(search : Option[String] = None, services : Option[String]= None, location : Option[LongLat]= None, minRating : Option[Double] = None,
//                   minReviews : Option[Integer]= None, minBadge : Option[Integer] = None, maxDistance : Option[Double] = None,
//                   orderBy : Option[String] = None, page : Int = 0): List[User] = {
//
//
//    val servicesCondition: Option[String] = services.map { x =>
//      x.split(",").map(s =>s""" (data->'providing'->>'services' like '%$s%') """).mkString(" or ")}.
//      map(y=>"(" + y + ")")
//
//
//    val condition = List(
//      search.map(x=> s" to_tsvector('English', data::text) @@ plainto_tsquery('English', '$x') "),
//      servicesCondition,
//      minRating.map(x=>s""" ${avgRatingSql()}  >= $x """.stripMargin),
//      minReviews.map(x=> s""" ${reviewsSql()}  >= $x """.stripMargin),
//      maxDistance.map(x=>s"""${distanceSql(location.get)}  < $x""".stripMargin),
//      minBadge.map(x=> s""" ($badgeSql >= $x) """.stripMargin),
//      Some(s"data->'providing' is not null " ))
//    val searchRule =condition.flatten.mkString(" and ")
//    println(searchRule)
//    val useeOpt = search.flatMap(s=>db(users).findOne[User](s))
//    println(useeOpt)
//    if (useeOpt.isDefined) {
//      List(useeOpt.get)
//    }
//    else {
//      val order = orderBy match {
//        case Some("reviews") => reviewsSql() + " desc "
//        case Some("distance") => distanceSql(location.get)
//        case Some("badge") => badgeSql + " desc "
//        case _ => avgRatingSql() + " desc "
//      }
//      val sql =
//        s"""
//           |select $users.data::text, ${avgRatingSql()} rating, ${reviewsSql()} reviews,
//           |${location.map(x=>distanceSql(x) + " distance").getOrElse(s"$VERY_FAR distance")},
//           |$badgeSql badge
//           |from $users
//           |where $searchRule
//           |order by $order
//           | offset ${page*10} limit 10
//      """.stripMargin
//      val rs = gconn.createStatement().executeQuery(sql)
//      var res = ArrayBuffer.empty[User]
//      while (rs.next()) {
//        val u= Extraction.extract[User](parse(rs.getString("data")))
//        res+=u.copy(stats = Some(ProviderStats(rs.getDouble("distance"),rs.getInt("reviews"),rs.getDouble("rating"),
//          getGrouping(rs.getDouble("distance"),rs.getInt("reviews"),rs.getDouble("rating"), rs.getInt("badge"),orderBy)
//        ))
//        )
//      }
//      rs.close()
//      res.toList
//    }
//  }


  def db(table : String ) = {
    SaveTable(table,this)
  }

}


case class SaveTable(table : String, p : UserPostgresPersistence) {

  implicit val formats = DefaultFormats
  def push[T](id: String, array: String, x: T) = {
    val j = Extraction.decompose(x)
    println(s"UPDATE $table set data  = jsonb_set(data::jsonb, array['$array'],(data->'$array')::jsonb || '[${compact(render(j))}]'::jsonb) where id = '$id'")
    p.executeUpdate(s"UPDATE $table set data  = jsonb_set(data::jsonb, array['$array'],(data->'$array')::jsonb || '[${compact(render(j))}]'::jsonb) where id = '$id' ")

  }
  def updateProp(id : String, prop: String) = {
    p.executeUpdate(s"UPDATE $table set data  = data::jsonb || '$prop'::jsonb where id = '$id' ")

  }

  def incProp(id : String, prop: String, value : String ) = {
    p.executeUpdate(s"""UPDATE $table set data  = data::jsonb || ('{"$prop":' || (COALESCE((data->>'$prop')::float,0.0) + $value) || '}')::jsonb where id = '$id' """)

  }

  def insertUserMizva (user : String,  x : UserMizvaUI): Unit = {
    //username varchar(50), mizva varchar(50),  duration int, quality int, notes varchar(400), created timestamp
    p.executeUpdate(s"insert into $table values ('${user}','${x.mizva}', ${x.extraInfo.map{y => y.duration}.getOrElse(0)}" +
      s", '${x.extraInfo.map{y => y.duration}.getOrElse("")}', now())")

  }

  def insertObligation (x : Obligation): Unit = {
    val j = Extraction.decompose(x)
    val id = (j \ "_id").extract[String]
    val toinsert = compact(render(j)).replace("'","''")
    p.executeUpdate(s"insert into $table values ('$id','${x.user}','${x.mizva}', '${toinsert}')")

  }

  def insert[T](x : T): Unit = {
    val j = Extraction.decompose(x)
    val id = (j \ "_id").extract[String]
    val toinsert = compact(render(j)).replace("'","''")
    p.executeUpdate(s"insert into $table values ('$id','${toinsert}')")

  }
  def update[T](x : T): Unit = {
    val j = Extraction.decompose(x)
    val id = (j \ "_id").extract[String]
    p.executeUpdate(s"insert into $table values ('$id','${compact(render(j))}')  " +
      s" ON CONFLICT (id)  DO UPDATE SET data = '${compact(render(j))}'")

  }

  def findOne[T](id : String)(implicit man: Manifest[T]): Option[T] = {
    p.getOneT[T]( s"select data from $table where id = '$id'")
  }

}


trait PostgresUtils  {
  val VERY_FAR = 1000000


  implicit val formats = DefaultFormats

  def gconn: Connection
  def executeUpdate(sql : String): Unit = {
    try {
      gconn.createStatement().execute(sql)
    }catch {
      case e: Exception =>
        println("problem executing sql:" + sql)
        throw e;
    }
  }


  def avgRatingSql(outcond : String = s"$users.id") =
    s"""
       |(COALESCE((select avg(($payments.data->>'rate')::integer) from $payments
       |where $payments.data->>'provider' = $outcond ),0))
    """.stripMargin

  def reviewsSql(outcond : String = s"$users.id") =
    s"""
       |(COALESCE((select count(($payments.data->>'rate')::integer) from $payments
       |where $payments.data->>'provider' = $outcond ),0))
     """.stripMargin

  def agentreviewsSql(outcond : String = s"$users.id") =
    s"""
       |(COALESCE((select count(($payments.data->>'rate')::integer) from $payments
       |where $payments.data->>'payer' = $outcond ),0))
     """.stripMargin

  def agentdealsSql(outcond : String = s"$users.id") =
    s"""
       |(COALESCE((select count(($payments.data->>'rate')::integer) from $payments
       |where $payments.data->>'referral' = $outcond ),0))
     """.stripMargin

  def distanceSql(location:LongLat) =
    s"""
       |(COALESCE((public.ST_Distance(
       |		public.st_transform( public.ST_GeomFromText('POINT(${location.longitude} ${location.latitude})',4326),2100),
       |  public.st_transform(public.ST_GeomFromText('POINT(' || ($users.data->>'longitude')::text || ' ' || ($users.data->>'latitude')::text || ')',4326),2100)
       |  )/1000),$VERY_FAR))
     """.stripMargin

  def badgeSql = s"""(COALESCE(($users.data->>'badge')::integer,1))"""

  def getRST[T](sql : String)(implicit man: Manifest[T]): List[T] = {
    val l = getRS(sql)
    l.map(x=>  parse(x).extract[T])
  }
  def getOneT[T]( sql : String)( implicit man: Manifest[T]): Option[T] = {
    val l = getGetOne(sql)
    val y= l.map(x=>  parse(x).extract[T])
    y
  }

  def  getRS(sql : String) : List[String] = {
    var res = ArrayBuffer.empty[String]
    val rs = gconn.createStatement().executeQuery(sql)
    while (rs.next()) {
      res+=rs.getString(1)
    }
    rs.close()
    return res.toList
  }
  def getEnriched(condition: String, tab: String): List[EnrichedClick] = {
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
    val sql =
      s"""
         |select target_rating, target_reviews, source, target, referral, _id, timestamp,
         |COALESCE(source_badge::integer,1) as source_badge,COALESCE(source_name,'') as source_name, COALESCE(source_img,'') as source_img,
         |COALESCE(target_badge::integer,1) as target_badge,COALESCE(target_name,'') as target_name, COALESCE(target_img,'') as target_img, target_services,
         |COALESCE(referral_badge::integer,1) as referral_badge,COALESCE(referral_name,'') as referral_name, COALESCE(referral_img,'') as referral_img
         |from (
         |select $tab.data->>'source' as source,$tab.data->>'target' as target ,$tab.data->>'referral' as referral ,$tab.data->>'_id' as _id, $tab.data->>'timestamp' as timestamp,
         | ${avgRatingSql(tab + ".data->>'target'")} target_rating, ${reviewsSql(tab + ".data->>'target'")} target_reviews,
         | usrc.data->>'badge' as source_badge  ,usrc.data->>'image' as source_img  ,(cast(usrc.data->>'name' as varchar) || ' ' || COALESCE(cast(usrc.data->>'lastname' as varchar),'' :: varchar))  as source_name,
         | utar.data->>'badge' as target_badge  ,utar.data->'providing'->>'services' as target_services  , utar.data->>'image' as target_img  ,(cast(utar.data->>'name' as varchar) || ' ' || COALESCE(cast(utar.data->>'lastname' as varchar),'' :: varchar))  as target_name,
         | uref.data->>'badge' as referral_badge ,uref.data->>'image' as referral_img  ,(cast(uref.data->>'name' as varchar) || ' ' || COALESCE(cast(uref.data->>'lastname' as varchar),'' :: varchar))  as referral_name
         | from $tab
         | LEFT JOIN users usrc ON (usrc.data->>'_id' = $tab.data->>'source')
         | LEFT JOIN users utar ON (utar.data->>'_id' = $tab.data->>'target')
         | LEFT JOIN users uref ON (uref.data->>'_id' = $tab.data->>'referral')
         | where $condition ) as foo order by timestamp
      """.stripMargin
    val rs = gconn.createStatement().executeQuery(sql)
    var res = ArrayBuffer.empty[EnrichedClick]
    while (rs.next()) {
      val d  = new Date(rs.getLong("timestamp"));
      res+=EnrichedClick(
        source = rs.getString("source"),
        target = rs.getString("target"),
        referral = rs.getString("referral"),
        id = rs.getString("_id"),
        timestamp = rs.getLong("timestamp"),
        timestamp_str = minuteFormat.format(d),
        source_str =  rs.getString("source_name"),
        target_str =  rs.getString("target_name"),
        referral_str =  rs.getString("referral_name"),
        source_img =  rs.getString("source_img"),
        target_img =  rs.getString("target_img"),
        referral_img =  rs.getString("referral_img"),
        target_services = Option(rs.getString("target_services")),
        source_badge = Some(rs.getInt("source_badge")),
        target_badge = Some(rs.getInt("target_badge")),
        referral_badge = Some(rs.getInt("referral_badge")),
        target_rating = Option(rs.getDouble("target_rating")),
        target_reviews = Option(rs.getInt("target_reviews"))
      )
    }
    rs.close()
    return res.toList
  }
  def getEndorsments(provider: String): List[EnrichedEndorsement] = {
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
    val sql =
      s"""
         |select user_name, _id, timestamp, COALESCE(user_full_name,'') as user_full_name, COALESCE(user_img,'') as user_img, service
         |from (
         |select p.data->>'user' as user_name,p.data->>'_id' as _id, p.data->>'timestamp' as timestamp, p.data->>'service' service,
         | usrc.data->>'image' as user_img  ,(cast(usrc.data->>'name' as varchar) || ' ' || COALESCE(cast(usrc.data->>'lastname' as varchar),'' :: varchar))  as user_full_name
         | from $endorsement p
         | LEFT JOIN users usrc ON (usrc.data->>'_id' = p.data->>'user')
         | where p.data->>'provider' = '$provider'
         | ) as foo order by timestamp
      """.stripMargin
    val rs = gconn.createStatement().executeQuery(sql)
    var res = ArrayBuffer.empty[EnrichedEndorsement]
    while (rs.next()) {
      val d  = new Date(rs.getLong("timestamp"));
      res+=EnrichedEndorsement(
        user = rs.getString("user_name"),
        provider = provider,
        user_str = rs.getString("user_full_name"),
        id = rs.getString("_id"),
        timestamp = rs.getLong("timestamp"),
        timestamp_str = minuteFormat.format(d),
        user_img =  rs.getString("user_img"),
        service = rs.getString("service")
      )
    }
    rs.close()
    return res.toList

  }

  def getEnrichedPayment(condition: String): List[EnrichedPayment] = {
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd")
    val sql =
      s"""
         |select source, target, referral, _id, timestamp,amount, rate, title, review,
         |COALESCE(source_badge::integer,1) as source_badge,COALESCE(source_name,'') as source_name, COALESCE(source_img,'') as source_img,
         |COALESCE(target_badge::integer,1) as target_badge, COALESCE(target_name,'') as target_name, COALESCE(target_img,'') as target_img,
         |COALESCE(referral_badge::integer,1) as referral_badge,COALESCE(referral_name,'') as referral_name, COALESCE(referral_img,'') as referral_img, target_services
         |from (
         |select p.data->>'payer' as source,p.data->>'provider' as target ,p.data->>'referral' as referral ,p.data->>'_id' as _id, p.data->>'timestamp' as timestamp,
         |       p.data->>'amount' as amount,p.data->>'rate' as rate ,p.data->>'title' as title ,p.data->>'review' as review,
         | usrc.data->>'badge' as source_badge  ,usrc.data->>'image' as source_img  ,(cast(usrc.data->>'name' as varchar) || ' ' || COALESCE(cast(usrc.data->>'lastname' as varchar),'' :: varchar))  as source_name,
         | utar.data->>'badge' as target_badge  ,utar.data->'providing'->>'services' as target_services  , utar.data->>'image' as target_img  ,(cast(utar.data->>'name' as varchar) || ' ' || COALESCE(cast(utar.data->>'lastname' as varchar),'' :: varchar))  as target_name,
         |  uref.data->>'badge' as referral_badge , uref.data->>'image' as referral_img  ,(cast(uref.data->>'name' as varchar) || ' ' || COALESCE(cast(uref.data->>'lastname' as varchar),'' :: varchar))  as referral_name
         | from $payments p
         | LEFT JOIN users usrc ON (usrc.data->>'_id' = p.data->>'payer')
         | LEFT JOIN users utar ON (utar.data->>'_id' = p.data->>'provider')
         | LEFT JOIN users uref ON (uref.data->>'_id' = p.data->>'referral')
         | where $condition ) as foo order by timestamp
      """.stripMargin
    val rs = gconn.createStatement().executeQuery(sql)
    var res = ArrayBuffer.empty[EnrichedPayment]
    while (rs.next()) {
      val d  = new Date(rs.getLong("timestamp"));
      res+=EnrichedPayment(
        source = rs.getString("source"),
        target = rs.getString("target"),
        referral = rs.getString("referral"),
        payment_id = rs.getString("_id"),
        timestamp = rs.getLong("timestamp"),
        timestamp_str = minuteFormat.format(d),
        source_str =  rs.getString("source_name"),
        target_str =  rs.getString("target_name"),
        referral_str =  rs.getString("referral_name"),
        source_img =  rs.getString("source_img"),
        target_img =  rs.getString("target_img"),
        referral_img =  rs.getString("referral_img"),
        amount = rs.getDouble("amount"),
        title = Option(rs.getString("title")),
        rate = if (rs.getObject("rate") == null) None else Some(rs.getInt("rate")) ,
        review = Option(rs.getString("review")),
        target_services = Option(rs.getString("target_services")),
        source_badge = Some(rs.getInt("source_badge")),
        target_badge = Some(rs.getInt("target_badge")),
        referral_badge = Some(rs.getInt("referral_badge"))
      )
    }
    rs.close()
    return res.toList
  }


  def  getGetOne(sql : String) : Option[String] = {
    val rs = gconn.createStatement().executeQuery(sql)
    val res = if (rs.next()) {
      Some(rs.getString(1))
    } else None
    rs.close()
    return res
  }
}
object test extends App {
  implicit val formats1 = DefaultFormats
  val ff =
    """
      |{"connections":[{"phone":"+972535353151","full_name":"Voice Mail"},{"phone":"050-200-7331","full_name":"???? ?????"},{"phone":"03-672-9003","full_name":"??? ?????"},{"phone":"036-116-262","full_name":"??? ???? ???????"},{"phone":"3462476","full_name":"???"},{"phone":"054-329-9986","full_name":"??? ????"},{"phone":"039-218-154","full_name":"???? ????"},{"phone":"03-502-0126","full_name":"????? ?????"},{"phone":"03-611-1463","full_name":"??? ?????????"},{"phone":"077-360-6018","full_name":"???? ???"},{"phone":"050-411-9143","full_name":"?"},{"phone":"057-812-7354","full_name":"??? ???"},{"phone":"+972 54-665-5717","full_name":"Ofer Genat"},{"phone":"036-168-866","full_name":"???"},{"phone":"03-611-1489","full_name":"???? ?????"},{"phone":"077-222-5822","full_name":"????? ????"},{"phone":"+972 3-910-1200","full_name":"????? ????? ?? ???????"},{"phone":"026-493-443","full_name":"?????? ?? ???"},{"phone":"03-924-4431","full_name":"?????"},{"phone":"054-924-4544","full_name":"????? ????"},{"phone":"039-223-970","full_name":"????? ??????"},{"phone":"072-228-7778","full_name":"???? ????"},{"phone":"052-623-3667","full_name":"???? ???????"},{"phone":"072-213-5809","full_name":"???? ???"},{"phone":"03-633-2033","full_name":"???? ??? ?????"},{"phone":"054-859-0444","full_name":"??? ???????"},{"phone":"052-702-5358","full_name":"???"},{"phone":"073-792-5427","full_name":"????? ????"},{"phone":"03-942-6039","full_name":"????? ?????"},{"phone":"+972 3-934-6685","full_name":"?? ????"},{"phone":"073-225-5101","full_name":"???? ??????"},{"phone":"+972 3-603-8092","full_name":"?? ??????"},{"phone":"+1 305-586-9968","full_name":"???? ???"},{"phone":"073-213-2413","full_name":"???? ?????"},{"phone":"039-255-303","full_name":"??? ????"},{"phone":"077-460-4618","full_name":"????? ??????"},{"phone":"03-921-8829","full_name":"??? ??? ?????"},{"phone":"03-645-0225","full_name":"??? ???"},{"phone":"073-206-9979","full_name":"???? ???????"},{"phone":"04-849-3311","full_name":"????"},{"phone":"073-234-2030","full_name":"???? ??????"},{"phone":"09-865-6226","full_name":"???????"},{"phone":"052-713-2360","full_name":"??????"},{"phone":"073-707-6309","full_name":"??????"},{"phone":"03-926-6621","full_name":"???? ?? ?? ???"},{"phone":"09-966-4110","full_name":"???? ?????"},{"phone":"099-541-946","full_name":"????? ????"},{"phone":"073-222-1218","full_name":"????"},{"phone":"03-918-0000","full_name":"???????? ????"},{"phone":"057-814-4209","full_name":"???? ?????"},{"phone":"03-611-1464","full_name":"?? ?????"},{"phone":"1-801-700-700","full_name":"???"},{"phone":"037-916-333","full_name":"? ????? ????"},{"phone":"09-971-5150","full_name":"???? ???"},{"phone":"054-692-3097","full_name":"???? ???"},{"phone":"077-921-0039","full_name":"??? ????"},{"phone":"036-364-333","full_name":"?????? ??????"},{"phone":"036-532-407","full_name":"?????? ??????"},{"phone":"03-542-4489","full_name":"???? ?????"},{"phone":"03-654-9579","full_name":"??? ?????"},{"phone":"0528934300+","full_name":"? ????"},{"phone":"03-504-7294","full_name":"???? ?????"},{"phone":"077-772-5666","full_name":"???? ?????"},{"phone":"050-787-4174","full_name":"???? ?????"},{"phone":"039-254-020","full_name":"????????"},{"phone":"073-320-4068","full_name":"???? ????"},{"phone":"052-763-2955","full_name":"?? ????? ????"},{"phone":"073-225-5109","full_name":"????"},{"phone":"03-372-2803","full_name":"??????"},{"phone":"09-971-5134","full_name":"??? ???"},{"phone":"099-553-360","full_name":"?????"},{"phone":"072-272-6635","full_name":"????"},{"phone":"052-762-7547","full_name":"??? ???????"},{"phone":"03-923-6920","full_name":"????"},{"phone":"052-888-9816","full_name":"????? ???"},{"phone":"054-425-9800","full_name":"??? ?? ????"},{"phone":"+972 52-557-1601","full_name":"?????? ??? ????"},{"phone":"03-740-6201","full_name":"????? ????? ?????"},{"phone":"+972 54-467-3664","full_name":"???? ?? ??"},{"phone":"03-675-3348","full_name":"????? ????? ??? ??"},{"phone":"02-649-3454","full_name":"??? ???????"},{"phone":"03-791-4774","full_name":"??? ???? ??"},{"phone":"074-719-8187","full_name":"????"},{"phone":"*2838","full_name":"?????? ??????"},{"phone":"039-420-413","full_name":"??? ??????"},{"phone":"+972 54-458-8552","full_name":"??? ???"},{"phone":"03-610-0309","full_name":"???"},{"phone":"03-542-4569","full_name":"????? ????"},{"phone":"03-636-4400","full_name":"??????? ???????"},{"phone":"03-542-4563","full_name":"????? ??? ?????"},{"phone":"074-732-0572","full_name":"???? ??"},{"phone":"+886928412154","full_name":"???? ???????"},{"phone":"1-700-506-070","full_name":"???????"},{"phone":"03-930-9833","full_name":"??? ??"},{"phone":"03-953-5656","full_name":"????? ???????"},{"phone":"*6272","full_name":"???????"},{"phone":"099-580-845","full_name":"?? ?????"},{"phone":"039-272-300","full_name":"??????"},{"phone":"03-610-0305","full_name":"?????"},{"phone":"054-919-4130","full_name":"??????? ???"},{"phone":"00 22","full_name":"??? ??????"},{"phone":"03-735-0010","full_name":"??????"},{"phone":"03-504-8752","full_name":"???? ????"},{"phone":"054-455-7287","full_name":"????? ?????"},{"phone":"073-226-5479","full_name":"???? ???"},{"phone":"03-696-5066","full_name":"?????"},{"phone":"073-233-1500","full_name":"??????? ???"},{"phone":"09-971-5143","full_name":"??? ???"},{"phone":"039-196-332","full_name":"???"},{"phone":"054-208-6708","full_name":"?? ????"},{"phone":"050-890-8090","full_name":"????????? ?????"},{"phone":"03-542-4571","full_name":"????? ???"},{"phone":"03-791-4203","full_name":"???? ????"},{"phone":"03-919-2666","full_name":"??????"},{"phone":"097798386","full_name":"????????"},{"phone":"039516663","full_name":"?????? ????? ?????"},{"phone":"0583245678","full_name":"??? ??? ??????"},{"phone":"0548109100","full_name":"??? ???? ?????????"},{"phone":"0526414446","full_name":"??? ???? ????"},{"phone":"0504190995","full_name":"??? ??? ???????"},{"phone":"039213003","full_name":"????? ???? ?? ???????"},{"phone":"036722122","full_name":"??? ???? ?????"},{"phone":"+972 52-442-4449","full_name":"??? - ???? ?? ???/????"},{"phone":"09-970-1216","full_name":"???? ?????"},{"phone":"03-633-4025","full_name":"?????? ?????"},{"phone":"+972 54-470-6709","full_name":"????? ??? ?? ??? (????)"},{"phone":"072-221-4455","full_name":"???? ??????"},{"phone":"+972 3-572-2273","full_name":"????? ?????? ???????"},{"phone":"03-512-2816","full_name":"????? ??? ?????"},{"phone":"+972 54-369-4818","full_name":"Saket Israel"},{"phone":"08-851-0999","full_name":"????? ????"},{"phone":"1-700-707-070","full_name":"??????? ????"},{"phone":"+972 54-296-9234","full_name":"?????"},{"phone":"7889171","full_name":"????"},{"phone":"08-915-0505","full_name":"??? ????"},{"phone":"+972 50-415-3532","full_name":"???? ???"},{"phone":"03-536-7069","full_name":"???? ???????"},{"phone":"+972 54-296-9235","full_name":"?????"},{"phone":"050-629-0707","full_name":"??? ????????"},{"phone":"054-243-4458","full_name":"????? ?????"},{"phone":"052-698-2529","full_name":"????? ????"},{"phone":"054-464-0880","full_name":"????? ????"},{"phone":"052-868-1414","full_name":"???? ??????"},{"phone":"054-657-6356","full_name":"????? ?????????"},{"phone":"+972 54-544-9916","full_name":"Yosi Janudi"},{"phone":"050-828-2583","full_name":"???? ??"},{"phone":"054-553-7887","full_name":"??? ???? ????"},{"phone":"052-694-6783","full_name":"???? ????"},{"phone":"+972 54-495-2995","full_name":"????? ?????"},{"phone":"054-235-3818","full_name":"??????? ????"},{"phone":"054-688-7870","full_name":"???? ??"},{"phone":"054-522-3704","full_name":"???? ???? ????"},{"phone":"053-336-7889","full_name":"?? ??"},{"phone":"050-461-5247","full_name":"???? ????"},{"phone":"050-528-5153","full_name":"???? ??????"},{"phone":"054-694-2007","full_name":"??????? ???"},{"phone":"052-854-2384","full_name":"??????? ????"},{"phone":"050-706-0003","full_name":"?????? ????"},{"phone":"054-485-7478","full_name":"???? ???"},{"phone":"052-878-4200","full_name":"?????"},{"phone":"052-408-2224","full_name":"??? ?????"},{"phone":"052-811-3030","full_name":"??? @ ???"},{"phone":"0502884952","full_name":"??? ???? ??? ?????"},{"phone":"054-545-7011","full_name":"???"},{"phone":"052-855-5477","full_name":"??? ???"},{"phone":"050-788-8403","full_name":"????? ????"},{"phone":"+972523600978","full_name":"????? ???"},{"phone":"+972 52-837-6880","full_name":"??? ??????"},{"phone":"054-203-2116","full_name":"???? ??????"},{"phone":"0544560308","full_name":"??? ???? ???????"},{"phone":"052-272-8755","full_name":"????? ??"},{"phone":"054-212-6685","full_name":"???? ?????"},{"phone":"050-688-1820","full_name":"??? ???"},{"phone":"054-537-4349","full_name":"??? ????????"},{"phone":"+972 54-744-4216","full_name":"????? ??"},{"phone":"052-684-4478","full_name":"??? ????"},{"phone":"054-482-8275","full_name":"???? ??????"},{"phone":"054-438-1104","full_name":"???? ???"},{"phone":"050-446-6436","full_name":"?????? ?????"},{"phone":"050-904-2122","full_name":"???? ???"},{"phone":"054-240-5275","full_name":"????? ???? ???"},{"phone":"052-569-7025","full_name":"???? ??? ????"},{"phone":"0542341881","full_name":"??? ???? ???"},{"phone":"052-500-0299","full_name":"??? ???"},{"phone":"052-854-2224","full_name":"???? ????"},{"phone":"074-717-0609","full_name":"???? ??"},{"phone":"052-801-3535","full_name":"?????? ???????"},{"phone":"+972 53-224-5017","full_name":"??????? ???"},{"phone":"+972 54-678-1730","full_name":"????? @ ?????"},{"phone":"0508910481","full_name":"??? ???? ????"},{"phone":"054-587-4799","full_name":"???? ????"},{"phone":"052-230-8690","full_name":"?? ????"},{"phone":"050-212-1070","full_name":"??? @ ????????"},{"phone":"050-778-3617","full_name":"?? ?????"},{"phone":"+972 54-638-9425","full_name":"???? ????"},{"phone":"+972 54-222-0621","full_name":"????"},{"phone":"+972 54-315-2929","full_name":"??? ?????"},{"phone":"+972 54-473-1449","full_name":"???? ?? ??? ???"},{"phone":"054-303-6370","full_name":"??????? ?????"},{"phone":"054-580-0668","full_name":"?? ??????????"},{"phone":"+886 909 304 087","full_name":"??????"},{"phone":"050-445-8136","full_name":"????????? ????"},{"phone":"050-570-2347","full_name":"???? ???"},{"phone":"0508765744","full_name":"??? ?? ????"},{"phone":"050-205-5100","full_name":"???? @ ????"},{"phone":"050-205-6859","full_name":"????"},{"phone":"050-420-0100","full_name":"????? ?????"},{"phone":"050-842-3045","full_name":"???? ????"},{"phone":"054-573-5335","full_name":"????? ???????"},{"phone":"054-313-0026","full_name":"???? ??? ???"},{"phone":"052-361-4043","full_name":"????? ????"},{"phone":"+972 52-243-3639","full_name":"???? ???? ??"},{"phone":"0544593401","full_name":"??? ???? ???????"},{"phone":"050-856-5536","full_name":"???? @ ????"},{"phone":"054-520-3204","full_name":"????????? ??"},{"phone":"054-252-2152","full_name":"??????? ???? ???"},{"phone":"054-303-0306","full_name":"???? ??????"},{"phone":"054-234-4146","full_name":"???? ??????"},{"phone":"054-804-4432","full_name":"???? ??? ?????"},{"phone":"054-494-9717","full_name":"????? ????"},{"phone":"050-630-0950","full_name":"??? ???"},{"phone":"+972 58-733-7557","full_name":"???? @???? ???"},{"phone":"052-468-5177","full_name":"?? ???? ?????"},{"phone":"0545607569","full_name":"??? ???? ????"},{"phone":"054-425-2426","full_name":"????"},{"phone":"+972545887147","full_name":"???? ????"},{"phone":"052-567-1882","full_name":"??? ???"},{"phone":"+972 54-447-7213","full_name":"??????? ???"},{"phone":"050-313-7556","full_name":"???? ???? ????"},{"phone":"052-315-3399","full_name":"????? ?????"},{"phone":"+972 52-326-6949","full_name":"???? ???????"},{"phone":"050-759-8261","full_name":"???? ??"},{"phone":"+972 50-430-3363","full_name":"????? ????????"},{"phone":"050-946-4633","full_name":"???? ?????"},{"phone":"052-823-5528","full_name":"????? ?? ????"},{"phone":"054-672-2446","full_name":"???? ????"},{"phone":"052-340-6836","full_name":"??? ?? ???"},{"phone":"052-325-8743","full_name":"?????"},{"phone":"050-434-3432","full_name":"????? @ ??? ??"},{"phone":"050-799-9241","full_name":"???? ?? ????"},{"phone":"054-666-7743","full_name":"???? ?????"},{"phone":"054-244-0944","full_name":"???? ?????"},{"phone":"058-667-3200","full_name":"???? @ ????"},{"phone":"054-212-4400","full_name":"???? ?????@ ????"},{"phone":"052-520-0727","full_name":"???? ?????"},{"phone":"050-407-8908","full_name":"??????? ????"},{"phone":"050-835-5224","full_name":"??? ?? ?? ???"},{"phone":"052-734-3842","full_name":"?????"},{"phone":"054-550-4716","full_name":"?????? ???"},{"phone":"+972 54-632-8736","full_name":"??? ?????????"},{"phone":"054-737-5036","full_name":"???? ????????"},{"phone":"054-738-8895","full_name":"??? @ ????"},{"phone":"+972 54-635-5886","full_name":"??????? ????"},{"phone":"052-844-5111","full_name":"??? ???"},{"phone":"+972 50-217-6555","full_name":"??? ????? ???? ???????"},{"phone":"0544447687","full_name":"??? ??? ??? (??"},{"phone":"052-678-0335","full_name":"?????"},{"phone":"054-676-8318","full_name":"??? ???"},{"phone":"054-397-5345","full_name":"??? ??????"},{"phone":"050-213-1335","full_name":"???? ????? ??????"},{"phone":"054-334-4192","full_name":"??? ?? ????"},{"phone":"054-483-4734","full_name":"??????? ???? ?????"},{"phone":"054-494-1748","full_name":"???? ????"},{"phone":"054-739-4668","full_name":"????? ????"},{"phone":"054-646-4948","full_name":"????@ ???"},{"phone":"054-452-0237","full_name":"????"},{"phone":"054-449-9501","full_name":"???? ???"},{"phone":"054-778-4343","full_name":"???? ?????"},{"phone":"052-641-3240","full_name":"???? ???????"},{"phone":"+972 52-899-4229","full_name":"???? ?????"},{"phone":"054-230-0588","full_name":"??????"},{"phone":"050-599-4955","full_name":"???? ???? ????? ??? ??"},{"phone":"054-770-7129","full_name":"??????? ????"},{"phone":"+972 54-480-5084","full_name":"???? ????"},{"phone":"+972 50-408-0418","full_name":"??? ?? ???? (???)"},{"phone":"050-680-2678","full_name":"???? ???"},{"phone":"054-949-7798","full_name":"???? ????? ????"},{"phone":"054-590-0194","full_name":"???? ?? ??????"},{"phone":"054-244-4350","full_name":"???? ??? ???"},{"phone":"052-351-4572","full_name":"????? ????"},{"phone":"+972 54-561-3373","full_name":"???? ????"},{"phone":"+972 54-465-9377","full_name":"??? ?????"},{"phone":"052-786-9223","full_name":"??? ???"},{"phone":"050-546-6006","full_name":"?????? ????"},{"phone":"054-765-0527","full_name":"?? ????"},{"phone":"054-284-8583","full_name":"????"},{"phone":"054-788-7707","full_name":"????? ????"},{"phone":"052-741-2888","full_name":"????? ???????"},{"phone":"052-452-7179","full_name":"????? ???? ????? ?????"},{"phone":"054-676-7992","full_name":"???? ???"},{"phone":"050-733-5412","full_name":"????? ?????"},{"phone":"052-701-3062","full_name":"??????? ????"},{"phone":"052-528-6092","full_name":"????????? ????? ??????"},{"phone":"054-305-5864","full_name":"???? ????"},{"phone":"+972 50-347-3355","full_name":"???? ???"},{"phone":"052-872-2763","full_name":"??? ????"},{"phone":"+972502454626","full_name":"Paul Seifer"},{"phone":"052-330-0371","full_name":"?????"},{"phone":"+972 54-492-2610","full_name":"???? ???"},{"phone":"+972 54-566-2270","full_name":"? ???? ?????"},{"phone":"0547313799","full_name":"??? ???? ?????"},{"phone":"052-474-7683","full_name":"????????? ???? ?????"},{"phone":"+972 52-539-7999","full_name":"??????? ????"},{"phone":"054-554-9455","full_name":"??? ?????"},{"phone":"052-681-5452","full_name":"???? ????"},{"phone":"+972 50-442-6378","full_name":"???? ???????? - ???"},{"phone":"054-790-1842","full_name":"? ???"},{"phone":"+972 54-474-3089","full_name":"???? ???"},{"phone":"072-336-9917","full_name":"???? ??????????"},{"phone":"+972 50-795-4618","full_name":"???? ???"},{"phone":"+972 54-332-3114","full_name":"??? ?? ????"},{"phone":"052-839-1147","full_name":"??? @ ???? ???????"},{"phone":"+972 54-542-4972","full_name":"???? ?????"},{"phone":"052-370-2277","full_name":"???? ???? ????"},{"phone":"+972 54-563-6811","full_name":"??????? ?? ?????"},{"phone":"050-706-0080","full_name":"?????? ?????"},{"phone":"052-397-5098","full_name":"???? ??????"},{"phone":"054-667-4077","full_name":"????"},{"phone":"052-565-5172","full_name":"??? ????????"},{"phone":"052-467-2090","full_name":"??????"},{"phone":"054-535-5141","full_name":"???? ????"},{"phone":"050-984-2011","full_name":"?????"},{"phone":"054-449-5352","full_name":"??? @ ???"},{"phone":"+972 54-435-7698","full_name":"???? ???????"},{"phone":"054-441-2356","full_name":"???? @ ???"},{"phone":"+972 50-900-1793","full_name":"???? ??????? ????"},{"phone":"054-476-9506","full_name":"???? ???"},{"phone":"052-617-3149","full_name":"????? ???????"},{"phone":"+972 54-338-3068","full_name":"???? ??? ???????"},{"phone":"050-696-6015","full_name":"??? ???? ??"},{"phone":"050-242-8896","full_name":"??? ?? ???"},{"phone":"052-224-1999","full_name":"??? ????"},{"phone":"+972523870007","full_name":"??? ???????"},{"phone":"050-706-0018","full_name":"??????? ??"},{"phone":"+972 54-598-5056","full_name":"????? ??????"},{"phone":"050-354-2200","full_name":"????? @ ???"},{"phone":"054-718-5959","full_name":"???? @ ???"},{"phone":"+972 54-423-1465","full_name":"???? ????? ????"},{"phone":"052-877-0141","full_name":"???? ????"},{"phone":"050-219-2190","full_name":"???? ???"},{"phone":"052-520-0727","full_name":"???"},{"phone":"052-802-9518","full_name":"????? @ ?????"},{"phone":"054-673-5021","full_name":"???? ?????? ????"},{"phone":"054-670-7570","full_name":"???? ?????"},{"phone":"052-397-2002","full_name":"???? ???? ???"},{"phone":"0528824445","full_name":"??? ???? ????????"},{"phone":"054-558-2218","full_name":"???"},{"phone":"+972 54-630-5103","full_name":"??? @ ????"},{"phone":"054-451-5800","full_name":"??????? ????"},{"phone":"050-651-2788","full_name":"???? ???"},{"phone":"053-708-6671","full_name":"?????? ????? ??????"},{"phone":"052-727-5335","full_name":"???? ???"},{"phone":"052-640-0431","full_name":"??? ?????"},{"phone":"052-897-9727","full_name":"????? ???? ???"},{"phone":"054-761-4694","full_name":"????? ??????"},{"phone":"054-999-8700","full_name":"???? @ ????"},{"phone":"050-302-0300","full_name":"???? ?????"},{"phone":"054-459-1033","full_name":"?????? ??"},{"phone":"0544950066","full_name":"??? ???? ???????"},{"phone":"050-215-7154","full_name":"???? ?????"},{"phone":"+972545843653","full_name":"???? ?? ???"},{"phone":"052-875-8138","full_name":"????@????"},{"phone":"052-854-9959","full_name":"??????? ????"},{"phone":"+972 54-669-0031","full_name":"??????"},{"phone":"054-456-2892","full_name":"?? @ ????"},{"phone":"052-703-5888","full_name":"????? ???? ???"},{"phone":"+972 54-756-5171","full_name":"??????"},{"phone":"+972 54-318-0038","full_name":"???? ???????"},{"phone":"0549992101","full_name":"??????? ???"},{"phone":"052-670-7777","full_name":"?????@ ????"},{"phone":"050-667-1212","full_name":"???? ????"},{"phone":"0545282043","full_name":"??? ?????? ?????"},{"phone":"+972 54-665-7535","full_name":"??? ????"},{"phone":"050-570-2036","full_name":"????? ?????????"},{"phone":"+972 52-739-2243","full_name":"???? ??? ???? ???????"},{"phone":"+972 54-770-6181","full_name":"????"},{"phone":"+972 52-364-6018","full_name":"???? ???? ????? ?? ???"},{"phone":"054-668-5855","full_name":"???? ???? ????????"},{"phone":"+972 54-648-0412","full_name":"??? ???"},{"phone":"+972543052555","full_name":"???? ??????"},{"phone":"052-569-7047","full_name":"??? ???????"},{"phone":"054-488-8250","full_name":"??????? ??? ???"},{"phone":"054-282-8997","full_name":"??? ????"},{"phone":"+972 52-375-3219","full_name":"??????? ???"},{"phone":"054-698-9092","full_name":"??? ????"},{"phone":"054-810-1234","full_name":"????? ?????"},{"phone":"054-626-6238","full_name":"???? ?????"},{"phone":"+972 54-487-8141","full_name":"???? ???? ?????????"},{"phone":"054-213-1564","full_name":"??? ????"},{"phone":"054-339-8046","full_name":"???? ???????"},{"phone":"055-881-0998","full_name":"???? ??????? ???"},{"phone":"050-420-4132","full_name":"????? ?????"},{"phone":"+972 54-792-7001","full_name":"???? ??? ?? ?????"},{"phone":"052-334-3067","full_name":"???? ?????"},{"phone":"054-336-6707","full_name":"????? ????"},{"phone":"+972 54-941-1188","full_name":"???? ??? ?? ????? ???? ? ????"},{"phone":"052-552-2748","full_name":"???? ?????"},{"phone":"08-949-2370","full_name":"???? ????"},{"phone":"054-495-9406","full_name":"?????? ???"},{"phone":"050-357-4050","full_name":"???? ????? ????"},{"phone":"054-979-2089","full_name":"???? ?????"},{"phone":"050-742-4948","full_name":"??? ???@ ?????"},{"phone":"054-488-6529","full_name":"???? ??????"},{"phone":"0547447988","full_name":"??? ????? ????????"},{"phone":"+972 54-797-1198","full_name":"??????? ??"},{"phone":"054-483-2318","full_name":"??? ?????"},{"phone":"050-667-0050","full_name":"??????? ????"},{"phone":"0524331255","full_name":"??? ????? ?? ???"},{"phone":"050-705-7041","full_name":"???? ???????"},{"phone":"+972 52-811-3117","full_name":"Mami"},{"phone":"050-733-7030","full_name":"??? ????"},{"phone":"+972 54-434-4479","full_name":"??????? ??? ?????"},{"phone":"052-667-7753","full_name":"??"},{"phone":"050-895-6977","full_name":"???? ???"},{"phone":"+972 54-477-8910","full_name":"?? ??? ????"},{"phone":"0526504534","full_name":"??? ???? ?????"},{"phone":"054-744-3029","full_name":"????"},{"phone":"052-260-5456","full_name":"??? ??????"},{"phone":"+972 54-592-4283","full_name":"? ????"},{"phone":"052-634-6747","full_name":"???? ????"},{"phone":"073-706-4432","full_name":"????? ????"},{"phone":"054-497-4120","full_name":"??? ??? ???????"},{"phone":"054-811-3265","full_name":"???@????"},{"phone":"050-533-8555","full_name":"????"},{"phone":"050-811-0440","full_name":"????? ???????"},{"phone":"054-793-0833","full_name":"????? @ ????"},{"phone":"052-678-9896","full_name":"??????? ???? ??????"},{"phone":"054-250-6600","full_name":"??????? ??????"},{"phone":"052-832-3102","full_name":"?? ?? @ ????"},{"phone":"052-577-5467","full_name":"????"},{"phone":"054-332-3120","full_name":"????? @ ????"},{"phone":"054-568-5848","full_name":"???? ????"},{"phone":"+972526148918","full_name":"???? ????????"},{"phone":"+972 52-477-9974","full_name":"????? ????"},{"phone":"0502345211","full_name":"??? ??? ???"},{"phone":"050-721-1281","full_name":"???? ???? ??????"},{"phone":"+972529203766","full_name":"??? ???? ???"},{"phone":"052-485-8999","full_name":"???? ?????"},{"phone":"052-353-3706","full_name":"????? ????"},{"phone":"054-777-7132","full_name":"???? ????"},{"phone":"+972 52-332-7241","full_name":"???? ?????"},{"phone":"050-330-8330","full_name":"????? ??? ???"},{"phone":"050-445-0528","full_name":"???? @ ???"},{"phone":"052-835-0033","full_name":"????"},{"phone":"0528797425","full_name":"??? ???? ????"},{"phone":"+972 54-922-2070","full_name":"???? ???"},{"phone":"054-437-8543","full_name":"???? ????"},{"phone":"0547320080","full_name":"??? ???? ?????"},{"phone":"054-522-5005","full_name":"???? ???"},{"phone":"+972 54-397-1969","full_name":"???? - ???? ????? ?????? ??"},{"phone":"054-342-3466","full_name":"???? ??? ???"},{"phone":"052-600-4382","full_name":"??????? ????"},{"phone":"+972 54-621-6653","full_name":"??????? ???"},{"phone":"054-435-5229","full_name":"????? @ ??????"},{"phone":"0526768855","full_name":"??? ??? ????"},{"phone":"052-534-0080","full_name":"??? ???? ?????"},{"phone":"+972 54-632-3990","full_name":"?????? ??? ????"},{"phone":"0544354963","full_name":"?? ??????"},{"phone":"050-404-9593","full_name":"???"},{"phone":"053-828-0966","full_name":"??? ?????"},{"phone":"054-262-2216","full_name":"????? ???? ?????"},{"phone":"050-706-0075","full_name":"??????? ?????"},{"phone":"054-681-6967","full_name":"???? ???? ????"},{"phone":"052-245-4643","full_name":"???"},{"phone":"052-552-3018","full_name":"??? ????"},{"phone":"050-485-4700","full_name":"??? @ ????"},{"phone":"052-821-1841","full_name":"????? ??? ?????"},{"phone":"052-883-0493","full_name":"?????? ?????"},{"phone":"054-580-7800","full_name":"????? @ ???"},{"phone":"052-295-2855","full_name":"???? ?????"},{"phone":"+972526180382","full_name":"? ???"},{"phone":"054-620-8221","full_name":"???? ????? ???????"},{"phone":"054-663-6642","full_name":"???? ?????"},{"phone":"054-737-0073","full_name":"???? ????"},{"phone":"052-852-8484","full_name":"??? ???"},{"phone":"052-556-7752","full_name":"??????? ???"},{"phone":"050-706-0015","full_name":"??????? ???"},{"phone":"054-330-7130","full_name":"???? ?????"},{"phone":"053-336-0705","full_name":"??? ????"},{"phone":"054-777-3080","full_name":"?????"},{"phone":"054-202-1621","full_name":"???? ???"},{"phone":"052-666-3639","full_name":"??? @ ?????"},{"phone":"054-563-4050","full_name":"??? ????"},{"phone":"050-874-3205","full_name":"??? ?????"},{"phone":"054-970-8075","full_name":"???? ????????"},{"phone":"054-301-2263","full_name":"? ????"},{"phone":"052-391-5558","full_name":"???? ???"},{"phone":"050-706-0060","full_name":"??????? ??? ??????"},{"phone":"050-706-0083","full_name":"??????? ???"},{"phone":"+972 54-445-2759","full_name":"????? ????'????"},{"phone":"054-662-0354","full_name":"??????? ????"},{"phone":"054-673-5805","full_name":"??? ?????"},{"phone":"+972 54-554-0983","full_name":"???? ????"},{"phone":"052-790-8692","full_name":"??????? ??? ????"},{"phone":"0507205006","full_name":"??? ???? ???"},{"phone":"052-387-7994","full_name":"? ???? ?????????"},{"phone":"054-645-2040","full_name":"????? ?????"},{"phone":"054-727-1415","full_name":"? ??"},{"phone":"+972 54-809-8600","full_name":"??? ?? ????"},{"phone":"058-647-8868","full_name":"?????"},{"phone":"054-224-2464","full_name":"???? ????"},{"phone":"052-672-7468","full_name":"????? ???? ????"},{"phone":"054-434-6089","full_name":"???"},{"phone":"054-582-3030","full_name":"???? ???"},{"phone":"050-339-9744","full_name":"?? ???????"},{"phone":"0504985155","full_name":"??? ??? ???"},{"phone":"+972 54-227-5828","full_name":"??????? ?????"},{"phone":"054-211-0856","full_name":"??? ?????? ?????"},{"phone":"055-664-6469","full_name":"??? ???????"},{"phone":"054-447-2020","full_name":"???? ???"},{"phone":"052-589-9222","full_name":"???? ?????"},{"phone":"+972 52-750-0343","full_name":"????? ????????"},{"phone":"050-880-7029","full_name":"??? ???"},{"phone":"054-539-9167","full_name":"???"},{"phone":"052-232-2555","full_name":"??????? ?????"},{"phone":"050-755-7738","full_name":"??? ???"},{"phone":"+972 54-660-3038","full_name":"???? @ ?????"},{"phone":"+972 52-669-0061","full_name":"????? ????"},{"phone":"054-626-6415","full_name":"?? ???????"},{"phone":"+972 50-690-6264","full_name":"?????? ???"},{"phone":"052-534-6377","full_name":"???? ????"},{"phone":"054-346-7483","full_name":"??? ??????"},{"phone":"052-867-4673","full_name":"???? ?????"},{"phone":"052-359-7267","full_name":"?????? ???"},{"phone":"052-569-7061","full_name":"???? ????"},{"phone":"+972544560063","full_name":"???? ?????"},{"phone":"+972 50-521-2936","full_name":"??? ???? ????"},{"phone":"054-331-4265","full_name":"?????? ???"},{"phone":"052-233-2330","full_name":"?? ???? ???? ???"},{"phone":"052-677-7864","full_name":"??????? ????"},{"phone":"0525477247","full_name":"??? ??? ????"},{"phone":"050-705-6588","full_name":"???? ???"},{"phone":"054-247-1173","full_name":"???? ???"},{"phone":"052-608-9095","full_name":"????? ???"},{"phone":"+972 54-815-7449","full_name":"????? @ ????"},{"phone":"054-423-4575","full_name":"???? ??? ????"},{"phone":"054-798-9651","full_name":"????? ???"},{"phone":"+972 52-364-4038","full_name":"??????? ????"},{"phone":"+972 52-840-4637","full_name":"????? ????"},{"phone":"052-581-0762","full_name":"?????"},{"phone":"054-574-8464","full_name":"???? ????"},{"phone":"054-991-4128","full_name":"???? ????"},{"phone":"+972 52-260-5455","full_name":"?? ???"},{"phone":"+972 54-245-4336","full_name":"???? ?????? Seisei"},{"phone":"054-761-6495","full_name":"????? ????????"},{"phone":"0523561540","full_name":"?????? ????"},{"phone":"054-731-6731","full_name":"???? ?????"},{"phone":"053-274-9735","full_name":"????? @ ????"},{"phone":"0545783387","full_name":"??? ???? ???"},{"phone":"+972 53-826-7311","full_name":"??? ?????"},{"phone":"054-523-3920","full_name":"?????? ?????"},{"phone":"052-222-6863","full_name":"?? ??????? ??????"},{"phone":"054-532-0320","full_name":"???? ??? ???"},{"phone":"053-333-0222","full_name":"??????"},{"phone":"054-555-7766","full_name":"??? ????"},{"phone":"0547658454","full_name":"?? ?????"},{"phone":"052-614-8634","full_name":"???? ?????"},{"phone":"052-870-2546","full_name":"??????"},{"phone":"0523743232","full_name":"??? ????? ????"},{"phone":"054-491-5002","full_name":"???? ?????"},{"phone":"054-312-5762","full_name":"????? @ ???"},{"phone":"052-745-9694","full_name":"???? ???"},{"phone":"+972 50-212-0424","full_name":"??????? ?????"},{"phone":"+972 54-900-0314","full_name":"??????? ????"},{"phone":"052-563-6337","full_name":"?????"},{"phone":"+972 54-699-9209","full_name":"??? ????"},{"phone":"+1 832-646-5223","full_name":"????? ????"},{"phone":"+972 54-632-2779","full_name":"????"},{"phone":"+972 52-891-4141","full_name":"???? ????"},{"phone":"+972 50-550-1949","full_name":"??? ???? ????"},{"phone":"+972 54-652-0660","full_name":"???? ???? ?????"},{"phone":"053-276-9957","full_name":"???? ???"},{"phone":"050-442-4420","full_name":"?? ???? ?????"},{"phone":"050-545-2045","full_name":"?????? ???? ??????"},{"phone":"+972 52-595-1073","full_name":"David Jewelry In Katsenelson"},{"phone":"+972 52-564-0757","full_name":"??????? ???"},{"phone":"054-216-1197","full_name":"???? ?????"},{"phone":"052-574-0911","full_name":"??? ??????"},{"phone":"052-896-4469","full_name":"???? ??????"},{"phone":"054-817-1187","full_name":"???? ????? ??????"},{"phone":"+972543361257","full_name":"??? ??????"},{"phone":"052-229-2288","full_name":"?? @ ????"},{"phone":"054-254-2828","full_name":"??????? @ ???? ?????"},{"phone":"0504640112","full_name":"??? ????? ????"},{"phone":"052-313-6242","full_name":"??? ??????"},{"phone":"050-687-3389","full_name":"???? ???????"},{"phone":"054-751-0923","full_name":"???? ?????"},{"phone":"052-354-1694","full_name":"???? ?????"},{"phone":"052-327-2299","full_name":"??? ???"},{"phone":"054-537-3732","full_name":"???? ?????"},{"phone":"050-266-9814","full_name":"??? ????"},{"phone":"052-829-6604","full_name":"???? ???"},{"phone":"054-772-2001","full_name":"???? @ ???"},{"phone":"054-740-8040","full_name":"??? ????"},{"phone":"052-955-1556","full_name":"???? ???????"},{"phone":"052-483-7464","full_name":"??? ???? (???"},{"phone":"052-393-3580","full_name":"????? ????"},{"phone":"03-687-2908","full_name":"???? ???"},{"phone":"050-404-0266","full_name":"??? ???"},{"phone":"+972 54-562-0614","full_name":"????? ??????"},{"phone":"050-354-1001","full_name":"??????? ?????"},{"phone":"052-355-5184","full_name":"? ?????"},{"phone":"050-542-8000","full_name":"????? ????? ???????"},{"phone":"052-257-5541","full_name":"???? ??? ???"},{"phone":"052-688-4034","full_name":"??????? ??????"},{"phone":"+972 50-621-2466","full_name":"???? ???? ???? ???????"},{"phone":"054-220-0908","full_name":"?????? ????"},{"phone":"+972 54-673-5733","full_name":"??? ?. ??????? ?????"},{"phone":"0503366243","full_name":"??? ??? ?????? ???"},{"phone":"+972 54-473-6590","full_name":"?????? ????"},{"phone":"054-727-5717","full_name":"???? ?????"},{"phone":"052-467-6003","full_name":"??? ???? ???"},{"phone":"+972 54-652-2655","full_name":"???? ???? ????"},{"phone":"052-619-4517","full_name":"???? ?????"},{"phone":"+972 50-739-0058","full_name":"??? ????? ?????"},{"phone":"+972 52-854-2691","full_name":"?????? ???"},{"phone":"0528875095","full_name":"??? ???? ?????"},{"phone":"054-771-6540","full_name":"??????? ????"},{"phone":"054-721-4492","full_name":"?????? ???"},{"phone":"054-648-8882","full_name":"?????"},{"phone":"0528662870","full_name":"??? ???? ????????"},{"phone":"+972 52-661-5296","full_name":"??? ????? ??? ????"},{"phone":"053-724-7958","full_name":"??? ???? ????"},{"phone":"054-724-8402","full_name":"???? ???????"},{"phone":"050-489-3377","full_name":"????"},{"phone":"0544668249","full_name":"??? ????? ?????????"},{"phone":"+972 52-348-4737","full_name":"??? ????? ????"},{"phone":"+972537739019","full_name":"??????"},{"phone":"+972 54-673-5682","full_name":"???? ???? ????? ???? ???"},{"phone":"050-733-4547","full_name":"????? ????"},{"phone":"050-260-0150","full_name":"??? ??????"},{"phone":"052-335-3351","full_name":"?? ???? ????"},{"phone":"0545909047","full_name":"??? ???? ?????"},{"phone":"054-729-1244","full_name":"???? ???"},{"phone":"054-751-2235","full_name":"??????? ?????"},{"phone":"054-434-5332","full_name":"??????? ???"},{"phone":"052-756-0772","full_name":"??? ???"},{"phone":"050-406-0064","full_name":"??? ???? ????"},{"phone":"054-568-6000","full_name":"??????? ????"},{"phone":"052-223-6236","full_name":"?????? ????"},{"phone":"054-580-5621","full_name":"???? ?????"},{"phone":"+972 54-664-4116","full_name":"???? ??????? ?????? ??????"},{"phone":"052-488-8565","full_name":"?????? ????"},{"phone":"054-799-8153","full_name":"???? ????? ?????? ????"},{"phone":"054-759-7887","full_name":"??????"},{"phone":"050-452-4691","full_name":"???? ????????"},{"phone":"054-474-4080","full_name":"???? ??? ?????"},{"phone":"+972 53-701-5024","full_name":"???? ?????"},{"phone":"0526834232","full_name":"??? ?????? ????????"},{"phone":"054-643-1973","full_name":"????? ????????"},{"phone":"052-354-9662","full_name":"??? @ ????"},{"phone":"050-970-0004","full_name":"??? @ ????"},{"phone":"+972 54-723-3626","full_name":"???? ???? ????"},{"phone":"054-452-4689","full_name":"????? ????"},{"phone":"+972 52-538-1450","full_name":"??????? ???"},{"phone":"052-632-3656","full_name":"????? ?"},{"phone":"+972 50-766-6660","full_name":"Eran Keinan"},{"phone":"+86 186 2194 4446","full_name":"????? ??? ?? ???"},{"phone":"+972533331853","full_name":"??? ?????"},{"phone":"050-887-4545","full_name":"??? ???"},{"phone":"054-304-8555","full_name":"???? ??? ?????"},{"phone":"054-449-4919","full_name":"???? ????? ??????"},{"phone":"+972544581620","full_name":"???? ????"},{"phone":"054-848-3032","full_name":"????? ??? ????"},{"phone":"058-588-7878","full_name":"?????? ???????"},{"phone":"+972 50-705-7821","full_name":"??????? ????"},{"phone":"054-674-1041","full_name":"???? ???? ??????"},{"phone":"052-563-6338","full_name":"???? ???? ??????????"},{"phone":"+972 50-741-4897","full_name":"????? ????"},{"phone":"052-631-3594","full_name":"??? ?? ???"},{"phone":"052-276-0577","full_name":"???"},{"phone":"+972 52-311-4265","full_name":"???? ??"},{"phone":"054-495-9407","full_name":"??? ???"},{"phone":"058-566-6223","full_name":"??????"},{"phone":"050-495-7706","full_name":"?????? ?????"},{"phone":"+972 54-431-1126","full_name":"???? ?????"},{"phone":"0523195544","full_name":"??? ???? ???????"},{"phone":"054-232-6573","full_name":"??????? ????"},{"phone":"+972 52-356-5682","full_name":"??? ?????"},{"phone":"052-382-4846","full_name":"???? ???"},{"phone":"052-828-3833","full_name":"??? ???"},{"phone":"+972 52-442-4449","full_name":"??? ????? ?? ???"},{"phone":"052-441-0664","full_name":"???? @ ???"},{"phone":"050-622-2495","full_name":"???? ?????"},{"phone":"054-486-8880","full_name":"??? ?????"},{"phone":"054-237-2717","full_name":"????? ????"},{"phone":"054-624-9923","full_name":"???? ????????"},{"phone":"+972 54-475-0889","full_name":"???? ????"},{"phone":"052-865-0230","full_name":"???? ?????"},{"phone":"052-447-4243","full_name":"??? ????"},{"phone":"052-847-2312","full_name":"???? @ ????"},{"phone":"052-884-2079","full_name":"???? ????"},{"phone":"073-707-6375","full_name":"??? ?????"},{"phone":"+972 54-442-2134","full_name":"?????? ???"},{"phone":"+972 52-614-8994","full_name":"??? ????"},{"phone":"0546306654","full_name":"??? ??? ????"},{"phone":"050-868-8893","full_name":"???? ?????? ????"},{"phone":"052-351-5555","full_name":"???? ?????"},{"phone":"054-209-4374","full_name":"????? ????"},{"phone":"054-481-0855","full_name":"??? ?? ?????"},{"phone":"052-869-5540","full_name":"???? ???"},{"phone":"050-728-3624","full_name":"???? ?????? ?????"},{"phone":"052-445-8845","full_name":"????? ????"},{"phone":"054-811-1711","full_name":"????????? ?????"},{"phone":"+972 52-680-1981","full_name":"???? ?????"},{"phone":"050-422-9008","full_name":"????? ????"},{"phone":"+972 54-770-1445","full_name":"???? ??????"},{"phone":"052-398-0999","full_name":"??? ?? ???? ????? (????"},{"phone":"054-478-3175","full_name":"????? ?????"},{"phone":"+972 58-479-9796","full_name":"??? ????"},{"phone":"053-866-3931","full_name":"????? ????? ??????"},{"phone":"+972 58-584-5545","full_name":"??? ???"},{"phone":"05282 75243","full_name":"??? ??????"},{"phone":"+972 50-871-3264","full_name":"??? ????? ???? ???"},{"phone":"052-311-2208","full_name":"??????? ?????"},{"phone":"053-708-2711","full_name":"???? @ ????????"},{"phone":"+972 54-490-5400","full_name":"????? ????"},{"phone":"+972 50-940-0420","full_name":"???? ?????"},{"phone":"053-708-2713","full_name":"???? @ ????????"},{"phone":"+972 58-687-7114","full_name":"Mae Bendavid"},{"phone":"+972 52-560-0154","full_name":"???? @ ?? ?? ????"},{"phone":"052-846-4922","full_name":"??????"},{"phone":"050-780-4741","full_name":"???? ?????"},{"phone":"054-625-0301","full_name":"????? @???? ??"},{"phone":"054-788-9900","full_name":"???? @ ???? ??"},{"phone":"054-292-6889","full_name":"??? @ ???? ??? ??"},{"phone":"054-467-7611","full_name":"Tia @ ???? ??? ??"},{"phone":"054-624-0002","full_name":"??? @ ???? ???? ??"},{"phone":"050-779-3899","full_name":"?? ??? @ ??? ???"},{"phone":"054-666-7080","full_name":"????? @ ???? ??"},{"phone":"052-872-5572","full_name":"?? ??? @ ??? ???"},{"phone":"052-890-0444","full_name":"?? ??? @ ?????"},{"phone":"052-249-8634","full_name":"?? ??? @ ?????"},{"phone":"050-755-9999","full_name":"?? ??? @ ???? ?????"},{"phone":"052-675-7417","full_name":"?? ??? @ ???? ?????"},{"phone":"054-488-8188","full_name":"?? ??? @ ???? ?? ????"},{"phone":"050-335-5233","full_name":"?? ??? @ ???? ?? ????"},{"phone":"050-649-7668","full_name":"?? ??? @ ?????"},{"phone":"052-846-8900","full_name":"?? ??? @ ?????"},{"phone":"050-766-5969","full_name":"?? ??? @ ???"},{"phone":"054-346-6446","full_name":"?? ??? @ ???"},{"phone":"050-788-1817","full_name":"?? ??? @ ???"},{"phone":"050-522-6222","full_name":"?? ??? @ ???"},{"phone":"054-754-7700","full_name":"?? ??? @ ???? ???"},{"phone":"054-754-7705","full_name":"?? ??? @ ???? ???"},{"phone":"058-718-2805","full_name":"????? @ ???"},{"phone":"050-706-0066","full_name":"??????? ?????"},{"phone":"052-325-0161","full_name":"???? @ ??????"},{"phone":"+972 52-770-7089","full_name":"????? ????"},{"phone":"+972 55-919-4741","full_name":"?????? ???? ?? ???"},{"phone":"+972 54-566-8998","full_name":"??? ??????"},{"phone":"+972 50-488-1811","full_name":"??????? ???"},{"phone":"+972 58-557-2270","full_name":"???? ??? @ ????"},{"phone":"+972 54-300-4220","full_name":"???? ????????? ?????"},{"phone":"052-233-3306","full_name":"??? ??? ??? ??????"},{"phone":"+972 54-523-0032","full_name":"King Solomon - Aaron"},{"phone":"054-488-0210","full_name":"????? ?????"},{"phone":"052-686-8788","full_name":"? ??? ???"},{"phone":"073-205-9642","full_name":"shapsali@gmail.com"},{"phone":"054-746-5557","full_name":"??????? ???"},{"phone":"052-111-1111","full_name":"Hhhh"},{"phone":"052-220-0143","full_name":"?????? ????"},{"phone":"054-266-5368","full_name":"????? ?????"},{"phone":"058-614-3164","full_name":"????? ????"}]}
      |
    """.stripMargin
  val f = parse( ff).extract[ConnList]
  println(f.connections.size)
//  val p = new UserPostgresPersistence("postgres", "postgres","postgres", "public", "52.15.136.250","5432")
//  val t = new UserPostgresPersistence("postgres", "postgres","postgres", "public", "52.15.136.250","5433")
//  val tbls = List(users,usertoken,chat,clicks,payviews,userviews,publishclicks,payments,posts,campaigns,notifications,earnings)
//  val rs= p.cs.executeQuery(s"select * from $connections")
//  while (rs.next()) {
//    t.cs.executeUpdate(s"insert into $connections values('${rs.getString("username")}'," +
//      s"'${rs.getString("phone")}','${rs.getString("full_name")}','${rs.getString("user2")}')")
//  }
//  rs.close()
//    val rs1= p.cs.executeQuery(s"select * from $campaigntouser")
//    while (rs1.next()) {
//      t.cs.executeUpdate(s"insert into $campaigntouser values('${rs1.getString("campaign_id")}'," +
//        s"'${rs1.getString("provider")}','${rs1.getString("username")}','${rs1.getString("accepted")}')")
//    }
//    rs1.close()

  //  tbls.foreach{
//    table =>
//      t.cs.execute(s"truncate table $table")
//      val data = p.getRS(s"select data from $table")
//      data.foreach{ d=>
//        val parsed = parse(d)
//        val id =   (parsed \ "_id").extract[String]
//        try {
//          t.cs.execute(s"insert  into $table values('$id','$d'::jsonb)")
//        }catch {
//          case e: Exception =>
//          println(s"insert  into $table values('$id','$d'::jsonb)")
//        }
//      }
//  }


//  PostgresObject.createTables("postgres")
  //println(p.getRS("select * from test"))
}

