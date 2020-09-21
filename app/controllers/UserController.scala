package controllers

import java.math.{BigDecimal => JBigDecimal}
import java.util.{Currency, UUID}

import com.firebase4s.database.Database
import com.github.tototoshi.play2.json4s.Json4s
import com.typesafe.scalalogging.LazyLogging
import org.json4s.DefaultFormats
import play.api.libs.ws.{WSAuthScheme, WSClient}

//import play.api.libs.json.Json
import akka.actor.ActorSystem
import akka.stream.Materializer
import javax.inject._
import org.json4s._
import org.json4s.native.Serialization.write
import persistence._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */


case class UserUrl(url : String)
case class UserPass(username : String, password : String)
case class Invite(reference : String, destinationType: String ,destination : String)
case class UserPassRef(username : String, password : String, fullname : String, email : String,   phone : String)
case class UserPassProviderRef(username : String, password : String, firstname : String,lastname : String, email : String,
          profession : Option[String], category : Option[String] , services : Option[String], phone : String, website : Option[String],  address : Option[String], referral : Option[String], longitude : Option[Double], latitude : Option[Double])
case class UserPassPublishAgentRef(username : String, password : String, firstname : String,lastname : String, email : String,
                                   sex : String, birthday : Long, family_status: String, no_children : Integer, household_income : String
                                   , phone : String, address : String, referral : Option[String],longitude : Option[Double], latitude : Option[Double])
case class UserUpdate(name : String, lastname : String, address : Option[String], phone : Option[String], email : Option[String], longitude : Option[Double], latitude : Option[Double])
case class ImageUrl( url : String)
case class PortfolioItemUI( image : String, title : Option[String], price : Option[String]) {


  def toPortfolio(provider : String) =  PortfolioItem(UUID.randomUUID().toString,provider,this.image,this.title,this.price)
}
case class PortfolioItem(_id : String, provider : String,  image : String, title : Option[String], price : Option[String])
case class Conn( phone : String, full_name : String)
case class ConnList( connections : List[Conn])

case class UpdatePassword( password : String)
case class ProviderLogin( referral : String)
case class ImageUrls( urls : List[String])
case class IncUserViews(referral : String)
case class IncPayViews(payer : Option[String], provider: String, referral : Option[String])
case class InviteSMS(src: String, dst : String, text : String)
case class NameValue(name: String, value : Integer)
case class NameSeries(name : String, series:List[NameValue])
case class PublishClick(provider: String)
case class NotificationClick(user: String)
case class InviteMessages(ask_referral: String, invite_user : String, invite_provider : String, publish_provider : String, pay_request : String )
case class PaytmFERequest( orderId : String, fromUser : Option[String], amount : BigDecimal)
case class LongLat(longitude: Double, latitude: Double)



@Singleton
  class UserController @Inject()( implicit actorSystem: ActorSystem,
                                  mat: Materializer, json4s: Json4s,cc: ControllerComponents,
                                  ws: WSClient,configuration: play.api.Configuration) extends AbstractController(cc) with LazyLogging{
    import json4s.implicits._

//  println("before writing")
//  val result =  fooRef.updateChildren(Map("niv3" ->"niv1"))
//  println(Await.result(fooRef.get(),Duration.Inf))
//  println(Await.result(result,Duration.Inf))
//  println("after writing")

//  val serviceAccount = getClass.getResourceAsStream("/ipublishu-198808-firebase-adminsdk-y0a3z-ca8c50a47f.json")
//  App.initialize(serviceAccount, "https://ipublishu-198808.firebaseio.com")

//  val db: Database = Database.getInstance()

  implicit val formats = new DefaultFormats {}
  val appPersistence  = new UserPostgresPersistence("postgres","postgres","postgres1","public", None, port = "2345")
                         //new UserPostgresPersistence("postgres","postgres","postgres")
                         //new UserMongoPersistence(DBName.publish)
  //val mongoUrlPersistence = new UrlMongoPersistence(DBName.publish)
  logger.info("creating indexed ")
  appPersistence.createIndexes();
  //mongoUrlPersistence.createIndexes();

  def addUserMizva(user : String) = Action(json4s.json) { implicit request =>
    safe (request , {
      logger.info("add user mizva called" + request.body.toString)
      val data = request.body.extract[UserMizvaUI]
      val provider = appPersistence.addUserMizva(user,data)
      Ok(Json.obj()).withHeaders(headers: _*)
    },curUser= Some(user))
  }

  def getUserMizvas(user : String) = Action { implicit request =>
    safeAny (request , {
      val res = appPersistence.getUserMizvas(user)
      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
    },curUser= None)
  }

  def addObligation(user : String) = Action(json4s.json) { implicit request =>
    safe (request , {
      logger.info("add obligation called" + request.body.toString)
      val data = request.body.extract[ObligationUI]
      val provider = appPersistence.addObligation(data)
      Ok(Json.obj("id"->provider._id)).withHeaders(headers: _*)
    },curUser= Some(user))
  }

  def getObligations(user: String) = Action { implicit request =>
    safeAny (request , {
      val res = appPersistence.getObligations(user)
      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
    },curUser= Some(user))
  }

  def addMizva() = Action(json4s.json) { implicit request =>
    safe (request , {
      logger.info("add mizva called" + request.body.toString)
      val data = request.body.extract[Mizva]
      val provider = appPersistence.addMizva(data)
      Ok(Json.obj("id"->provider._id)).withHeaders(headers: _*)
    },curUser= None)
  }

  def getMizvas() = Action { implicit request =>
    safeAny (request , {
      val res = appPersistence.getMizvas()
      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
    },curUser= None)
  }

//  def endorse(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("add endorse called" + request.body.toString)
//      val data = request.body.extract[EndorsementUI]
//      val end = Endorsement(id,data.provider,data.service,System.currentTimeMillis(), UUID.randomUUID().toString)
//      appPersistence.endorse(end)
//      Ok(Json.obj()).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def review(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      appPersistence.getProvider(id).map{p=>
//        logger.info("add review called" + request.body.toString)
//        val data = request.body.extract[ReviewUI]
//        val end = Review(id,data.user,data.rate,data.review,UUID.randomUUID().toString,System.currentTimeMillis())
//        appPersistence.addreview(end)
//        Ok(Json.obj()).withHeaders(headers: _*)
//      }.getOrElse(Forbidden(Json.obj()).withHeaders(headers: _*))
//    },curUser= Some(id))
//  }
//  def inviteAgent(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("invite agent called" + request.body.toString)
//      val data = request.body.extract[InviteCampaignUI]
//      appPersistence.inviteToCampaign(data.campaign_id,id,data.usernames)
//      Ok(Json.obj()).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def acceptAgent(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("accept agent called" + request.body.toString)
//      val data = request.body.extract[AcceptCampaignUI]
//      appPersistence.acceptCampaign(data.campaign_id,id,data.username)
//      Ok(Json.obj()).withHeaders(headers: _*)
//    },curUser= None)
//  }
//
//  def getCampaigns(id : String) = Action { implicit request =>
//    safeAny (request , {
//      val res = appPersistence.getCampaigns(id)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def getOffers(id : String) = Action { implicit request =>
//    safeAny (request , {
//      val res = appPersistence.getUserOffers(id)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def getCampaign(id : String, campaign_id : String) = Action { implicit request =>
//    safeAny (request , {
//      val res = appPersistence.getCampaign(id, campaign_id)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//
//  def userPost(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("user post click called" + request.body.toString)
//      val data = request.body.extract[UIPost]
//      val provider = appPersistence.addUserPost(UserPost(UUID.randomUUID().toString,
//        id,data.image,data.post_text,System.currentTimeMillis()
//      ))
//      Ok(Json.obj()).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def getSocialFeed(id : String) = Action { implicit request =>
//    safeAny (request , {
//      val res = appPersistence.getSocialFeed(id)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def getProviderPosts(id : String) = Action { implicit request =>
//      val res = appPersistence.getProviderPosts(id)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//  }
//
//  def quickSearch(id : String, search : String) = Action { implicit request =>
//    safeAny (request , {
//      val res = appPersistence.quickSearch(id,search)
//      Ok(Extraction.decompose(res)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }

//  def imagefolder() = configuration.underlying.getString("image.folder")
  def login() = Action(json4s.json) { implicit request =>
    safe (request , {
      logger.info("login called " + request.body.toString)
      logger.info("login called")
      val userpass = request.body.extract[UserPass]
      val user = appPersistence.getUser(userpass.username)
      if (user.map(u => u.password.get == userpass.password).getOrElse(false)) {
        logger.info("login success")
        logger.info("login success!!")
        Ok(Json.obj("token" -> appPersistence.generateToken(userpass.username))).withHeaders(headers: _*)
      } else {
        logger.info("login denied")
        logger.info("login denied")
        NotFound.withHeaders(headers: _*)
      }
    },auth = false)
  }
  def verifylogin() = Action(json4s.json) { implicit request =>
    safe (request , {
      logger.info("verify login called " + request.body.toString)
      val userpass = request.body.extract[UserToken]
      val user = appPersistence.getUserForToken(userpass.token)
      if (user.map(u=>u == userpass.username).getOrElse(false)) {
        logger.info("verify success")
        logger.info("verify success!!")
        Ok(Json.obj()).withHeaders(headers: _*)
      }else {
        logger.info("verify login denied")
        logger.info("verify login denied")
        NotFound.withHeaders(headers: _*)
      }
    },auth = false)
  }

//  val OTPs = TrieMap.empty[String, String]
//  val digits = ('0' to '9')
//  def sendOTP() = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("sendOTP called " + request.body.toString)
//      val sendotp = request.body.extract[SendOTP]
//      val otp = (1 to 6).map(i=> digits(Random.nextInt(digits.length))).mkString
//      OTPs.put(sendotp.user + sendotp.calltype, otp.mkString)
//      if (Set("register","registerProvider","registerPublishAgent").contains(sendotp.calltype)) {
//        sendSms(Invite("your code for ipublishu is:" + otp, "", sendotp.phone.getOrElse("")))
//        Ok(Json.obj()).withHeaders(headers: _*)
//      }
//      else {
//        appPersistence.getUser(sendotp.user).map{ u=>
//          sendSms(Invite("your code for ipublishu is:" + otp,"",u.phone.getOrElse("")) )
//          Ok(Json.obj()).withHeaders(headers: _*)
//        }.getOrElse( NotFound.withHeaders(headers: _*))
//      }
//
//    },auth = false)
//  }
//  def verifyOTP() = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("verifyOTP called " + request.body.toString)
//      val ot: VerifyOTP = request.body.extract[VerifyOTP]
//      if (checkOtp(request, ot.user, ot.calltype))
//        Ok(Json.obj()).withHeaders(headers: _*)
//      else
//        Unauthorized.withHeaders(headers: _*)
//    },auth = false)
//  }
//
//  def verifyloginWithGoogle() = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("verify login called " + request.body.toString)
//      val emailToken = request.body.extract[GoogleToken]
//      import java.util.Collections
//      val usr: Option[String] = appPersistence.getUserForEmail(emailToken.email)
//      logger.info(usr.toString)
//      if (usr.map{ u => GoogleVerifier.verify(emailToken.token) }.getOrElse(false)) {//
//        logger.info("login success")
//        logger.info("login success!!")
//        Ok(Json.obj("token" -> appPersistence.generateToken(usr.get), "user" -> usr.get)).withHeaders(headers: _*)
//      } else {
//        logger.info("login denied")
//        logger.info("login denied")
//        NotFound.withHeaders(headers: _*)
//      }
//    },auth = false)
//  }
//
//
//  def invite() = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("invite called" + request.body)
//      val invite = request.body.extract[Invite]
//      if (invite.destinationType == "WHATSUP") {
//        val q = s"?token=dde96bd34b6addc58f248b751c4602a35a5dabc7c1449&uid=972525236451&to=${invite.destination}&custom_uid=${UUID.randomUUID().toString}&text=${URLEncoder.encode(invite.reference,"UTF-8")}"
//
//        var req = ws.url("https://www.waboxapp.com/api/send/chat" + q)
//        req.execute("POST").map(x=> logger.info("whatsup res:" +  x.body))
//        Ok(Json.obj()).withHeaders(headers: _*)
//      }
//      else {
//        sendSms(invite)
//        Ok(Json.obj()).withHeaders(headers: _*)
//
//      }
//    })
//
//  }

  private def sendSms(invite: Invite) = {
    var req = ws.url("https://api.plivo.com/v1/Account/MAYME4NDNJNTJHZMUZNZ/Message/").
      withAuth("MAYME4NDNJNTJHZMUZNZ", "ZWM4YTc1ZDhlOTI0ZjU2ODM4NTJmYTdmMjRlMzNk", WSAuthScheme.BASIC).
      withHttpHeaders("Content-Type" -> "application/json").
      withBody(write(Extraction.decompose(InviteSMS("111111111", invite.destination, invite.reference))))
    req.execute("POST").map(x => logger.info("sms res:" + x.body))
  }

  def safe(req : Request[JValue], c : => Result, auth : Boolean= true, curUser : Option[String]=None)  ={
      try {
        if (auth) {
          val token = req.headers.toMap("token")(0)
          val user= appPersistence.getUserForToken(token)
          if (!(user.isDefined)){
              throw new RuntimeException("authentication error")
          }
          curUser.foreach(u=>
            if (u != user.getOrElse("")) {
              throw new RuntimeException("called by unauthorized")
            }
          )
        }

        c
      }
    catch {
      case e: Throwable  =>
        e.printStackTrace();
        BadRequest.withHeaders(headers : _*)
    }
  }
  def safeAny(req : Request[AnyContent], c : => Result, auth : Boolean= true, curUser : Option[String]=None)  ={
    try {
      if (auth) {
        val token = req.headers.toMap("token")(0)
        val user= appPersistence.getUserForToken(token)
        if (!(user.isDefined)){
          throw new RuntimeException("authentication error")
        }
        curUser.foreach(u=>
          if (u != user.getOrElse("")) {
            throw new RuntimeException("called by unauthorized")
          }
        )
      }
      c
    }
    catch {
      case e: Throwable  =>
        e.printStackTrace();
        BadRequest.withHeaders(headers : _*)
    }
  }
  def register(): Action[JValue] = Action(json4s.json) { implicit request =>
    safe (request ,{
      logger.info("register called" + request.body.toString)
      val userpass = request.body.extract[UserPassRef]
      logger.info("register extracted")


      val user = appPersistence.getUser(userpass.username)
      logger.info("register user1")
      if (user.isDefined) {
        Conflict.withHeaders(headers: _*)
      }
      else {

          logger.info("register user2")


          logger.info("register success")
          val token = appPersistence.addUser(User(_id = userpass.username, name = userpass.fullname,  password = Some(userpass.password),
            email = Some(userpass.email), referral = Some(""), image = Some("images_rjiphc")))
          Ok(Json.obj("token" -> token)).withHeaders(headers: _*)

      }
    },auth = false);
  }
//
//  private def checkOtp(request: Request[JValue], user: String, calltype : String) : Boolean = {
//    if (request.headers.toMap.get("otp").isEmpty) {
//      logger.info("request otp was empty")
//      return false
//    }
//    logger.info("otp is: "+ request.headers.toMap.get("otp").get(0))
//    request.headers.toMap.get("otp").get(0) == OTPs.get(user + calltype).getOrElse("")
//  }
//
//  def registerProvider(): Action[JValue] = Action(json4s.json) { implicit request =>
//    safe (request ,{
//      logger.info("register called" + request.body.toString)
//      val userpass = request.body.extract[UserPassProviderRef]
//      logger.info("register extracted")
//      val user = appPersistence.getUser(userpass.username)
//      logger.info("register user1, existing is:"  +user)
//      if (user.isDefined) {
//        Conflict.withHeaders(headers: _*)
//      }else  if (!checkOtp(request, userpass.username,"registerProvider")) {
//        Unauthorized.withHeaders(headers: _*)
//      }
//      else {
//        logger.info("not defined")
//        val refer = appPersistence.getUser(userpass.referral.getOrElse(""))
//        if ((userpass.referral.getOrElse("")!="") && !refer.isDefined) {
//          logger.info("not acceptable")
//          NotAcceptable.withHeaders(headers: _*)
//        }else {
//          logger.info("register user provider")
//          val token = appPersistence.addUser(User(_id = userpass.username,  name = userpass.firstname, lastname = Some(userpass.lastname), password = Some(userpass.password),
//            email = Some(userpass.email),address = userpass.address, longitude = userpass.longitude, latitude = userpass.latitude, phone= Some(userpass.phone), referral = userpass.referral, image = Some("images_rjiphc"),
//            providing = Some(Provider(profession = userpass.profession,category = userpass.category, services = userpass.services, website = userpass.website))
//          ))
//          logger.info("got token")
//          refer.foreach(x=>appPersistence.addNotification(x._id, s"${userpass.firstname} ${userpass.lastname} has registered as provider by your reference, you may publish ${userpass.firstname} by this link: ipublishu.com/welcome?action=ask_referral&provider=${userpass.username} "))
//          Ok(Json.obj("token" -> token)).withHeaders(headers: _*)
//        }
//      }
//    }, auth = false);
//  }
//  def registerPublishAgent(): Action[JValue] = Action(json4s.json) { implicit request =>
//    safe (request ,{
//      import java.time.Year
//      val year = Year.now.getValue
//      logger.info("register called" + request.body.toString)
//      val userpass = request.body.extract[UserPassPublishAgentRef]
//      logger.info("register extracted")
//      val user = appPersistence.getUser(userpass.username)
//      logger.info("register user1, existing is:"  +user)
//      if (user.isDefined) {
//        Conflict.withHeaders(headers: _*)
//      }else  if (!checkOtp(request, userpass.username,"registerPublishAgent")) {
//        Unauthorized
//      }
//      else {
//        logger.info("not defined")
//        val refer = appPersistence.getUser(userpass.referral.getOrElse(""))
//        if ((userpass.referral.getOrElse("")!="") && !refer.isDefined) {
//          logger.info("not acceptable")
//          NotAcceptable.withHeaders(headers: _*)
//        }else {
//          logger.info("register user provider")
//          val token = appPersistence.addUser(User(_id = userpass.username, name = userpass.firstname, lastname = Some(userpass.lastname), password = Some(userpass.password),
//            email = Some(userpass.email),address = Some(userpass.address), longitude = userpass.longitude, latitude = userpass.latitude, phone= Some(userpass.phone), referral = userpass.referral, image = Some("images_rjiphc"),
//            publishagent = Some(PublishAgent(sex= userpass.sex,birthday = year- userpass.birthday,family_status = userpass.family_status,no_children = userpass.no_children,household_income = userpass.household_income))
//          ))
//          logger.info("got token")
//          refer.foreach(x=>appPersistence.addNotification(x._id, s"${userpass.firstname} ${userpass.lastname} has registered as provider by your reference, you may publish ${userpass.firstname} by this link: ipublishu.com/welcome?action=ask_referral&provider=${userpass.username} "))
//          Ok(Json.obj("token" -> token)).withHeaders(headers: _*)
//        }
//      }
//    }, auth = false);
//  }
//
//  def notifyProviderLogin(id : String): Action[JValue] = Action(json4s.json) { implicit request =>
//    safe (request ,{
//      logger.info("notifyProviderLogin called" + request.body.toString)
//      val user = request.body.extract[ProviderLogin]
//      val refer = appPersistence.getUser(user.referral)
//      val target = appPersistence.getUser(id)
//      refer.foreach(x=>appPersistence.addNotification(x._id, s"${target.map(t => t.name).getOrElse("")} ${target.flatMap(t=>t.lastname).getOrElse("")} has registered as provider by your reference, you may publish ${target.map(t=> t.name).getOrElse("")} by this link: www.ipublishu.com/user/john?action=publish_provider&referral=mia&provider=${id} "))
//      Ok(Json.obj())
//    });
//  }

  def removePassword(value: JValue) = {
    value removeField {
      case ("password", v) => true
      case ("referral", v) => true
      case ("withdrawAccounts", v) => true
      case _ => false
    }
  }

  def removeSensitive(value: JValue) = {
    value removeField {
         case ("password", v) => true
         case ("pay_referral", v) => true
         case ("pay_payed", v) => true
         case ("provider_views", v) => true
         case ("reffered_payments_views", v) => true
         case ("reffered_users_views", v) => true
         case ("reffered_users", v) => true
         case ("publish_clicks", v) => true
         case ("messages", v) => true
         case ("notifications", v) => true
         case ("notifications_new", v) => true
         case ("withdrawAccounts", v) => true
         case ("balance", v) => true
         case ("earnings", v) => true
         case ("lastSeenNotification",v) => true
         case ("notification_id",v) => true
         case _ => false
    }
  }
//
//  def getProviders(search : Option[String], services : Option[String],longlat : Option[String],minRating : Option[Double],
//                   minReviews : Option[Integer], minBadge : Option[Integer], maxDistance : Option[Double],
//                   orderBy : Option[String],
//                   page : Int) = Action { implicit request =>
//    safeAny (request , {
//      logger.info("search called " + search + " "  + services + " " + longlat)
//      var location  : Option[LongLat]= None;
//      Try{location = longlat.map(x=>JsonMethods.parse(x).extract[LongLat])}
//      val users = appPersistence.getProviders(search, services,location, minRating, minReviews,minBadge,maxDistance,orderBy, page)
//      Ok(removeSensitive( Extraction.decompose(users))).withHeaders(headers: _*)
//    })
//  }
//
//  def getPublishAgents(sex : Option[String],  family_status : Option[String],
//                       household_income : Option[String], age_groups : Option[String],
//                       minDeals : Option[Integer] = None ,
//                       longlat : Option[String]= None,
//                       minReviews : Option[Integer]= None, minBadge : Option[Integer] = None, maxDistance : Option[Double] = None,
//                       orderBy : Option[String] = None,
//                       page : Int) = Action { implicit request =>
//    safeAny (request , {
//      logger.info("search publishagent called " + sex )
//      var location  : Option[LongLat]= None;
//      Try{location = longlat.map(x=>JsonMethods.parse(x).extract[LongLat])}
//      val users = appPersistence.getPublishAgents(sex,family_status,household_income ,age_groups,
//        minDeals= minDeals, location = location,minReviews= minReviews,minBadge=minBadge,maxDistance=maxDistance,orderBy=orderBy,
//        page = page)
//      Ok(Extraction.decompose(users)).withHeaders(headers: _*)
//    })
//  }
//
//  def getProvider(id : String) = Action { implicit request =>
//      logger.info("get provider called")
//      val users = appPersistence.getProvider(id)
//      val reverseUser = users.map(x=>x.copy(pay_received = x.pay_received.reverse))
//      reverseUser.map(u => Ok(removeSensitive( Extraction.decompose(u))).withHeaders(headers: _*)).
//        getOrElse(NotFound.withHeaders(headers: _*))
//  }
//
//  def getOnlineDetails(id : String) = Action { implicit request =>
//    safeAny (request , {
//
//      val users = appPersistence.getUser(id)
//      users.map(u => Ok(Json.obj("image" -> u.image, "lastseen" -> u.lastseen.map(x => System.currentTimeMillis() - x), "name" -> (u.name + " " + u.lastname.getOrElse("")))).withHeaders(headers: _*)).
//        getOrElse(NotFound.withHeaders(headers: _*))
//    })
//  }
//
//  case class OnlineDetailsO(image : String, lastseen : Long, name : String )
//  def getOnlineDetails2(ids : String) = Action { implicit request =>
//    safeAny (request , {
//      val u = ids.split(",").map{id =>
//        val users = appPersistence.getUser(id)
//        users.map(u =>
//          OnlineDetailsO( u.image.getOrElse(""),  u.lastseen.map(x => System.currentTimeMillis() - x).getOrElse(1000000), (u.name + " " + u.lastname.getOrElse(""))))
//
//      }.flatten
//      Ok(Extraction.decompose(u)).withHeaders(headers: _*)
//    })
//  }
//
//  def getChatsForUser(id : String) = Action { implicit request =>
//    logger.info("get online details called")
//    safeAny (request , {
//
//      val users = appPersistence.getChatsForuser(id).map(x=>MessagesEnriched.fromMassages(x,appPersistence))
//
//      Ok(Extraction.decompose(users)).withHeaders(headers: _*)
//    },curUser = Some(id))
//  }
//
//  def addInviteFields() = {
//    InviteMessages(
//      ask_referral = "Hi, I have registered as a provider on ipublishu.com where users get commission for publishing providers on social networks, please follow this link and learn how to get rewarded from publishing my services : [link]",
//      invite_user = "Hi, I have registered to ipublishu.com where users get commission for publishing providers on social networks, you may also register via this link: [link]",
//      publish_provider = "Hi, I had a great experience with [name] from ipublishu.com, warmly recommend using his services via this link: [link]",
//      invite_provider = "Hi I recommend you check ipublishu.com, where you can get your business promoted via social networks, if interested please follow this link: [link]" ,
//      pay_request = "Thanks for using my services, you can pay me via this link: [link]"
//    )
//  }

  def getUser(id : String) = Action { implicit request =>
    safeAny (request , {
      import java.time.Year
      val year = Year.now.getValue

      logger.info("getUser ")
      val user = appPersistence.getUser(id, true)
      logger.info(user.toString)
      val latstTenProviders = user.map(x=>x.publish_clicks).getOrElse(List()).groupBy(_.target)
        .map(_._2.maxBy(_.timestamp))
        .toList.sortBy(x=>x.timestamp).reverse
      val age = user.map(x=>x.publishagent.map(y=> year - y.birthday)).flatten
      user.map(x=>x.copy(publish_clicks = latstTenProviders.take(10),
        publishagent = x.publishagent.map(x=>x.copy(birthday = age.getOrElse(0))),
        notifications = x.notifications.reverse,
        pay_payed = x.pay_payed.reverse, pay_received = x.pay_received.reverse, pay_referral = x.pay_referral.reverse,
        messages = None)).
        map(u =>  Ok(removePassword(Extraction.decompose(u))).withHeaders(headers: _*)).
        getOrElse(NotFound.withHeaders(headers: _*))
    },curUser= Some(id))
  }
//
//  def getServices() =  Action { implicit request =>
//
//      Ok(Extraction.decompose(services)).withHeaders(headers: _*)
//
//  }
//
//  def publishClick(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("getUser ")
//      logger.info("publish click called" + request.body.toString)
//      val data = request.body.extract[PublishClick]
//      val provider = appPersistence.getUser(data.provider)
//      provider.map{p =>
//        appPersistence.publishClick(id,data.provider)
//        Ok(Json.obj()).withHeaders(headers: _*)
//      }.getOrElse(NotFound.withHeaders(headers: _*))
//    },curUser= Some(id))
//  }
//
//  def notificationClick(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("getUser ")
//      logger.info("notification click called" + request.body.toString)
//      val user = appPersistence.getUser(id)
//      user.map{p =>
//        appPersistence.notificationClick(id,user.map(x=>x.notifications.size).getOrElse(0))
//        Ok(Json.obj()).withHeaders(headers: _*)
//      }.getOrElse(NotFound.withHeaders(headers: _*))
//    },curUser= Some(id))
//  }
//
//  def minifyUrl(id : String) = Action(json4s.json) { implicit request =>
//    safe (request , {
//      logger.info("minify ")
//      logger.info("minify called" + request.body.toString)
//      val data = request.body.extract[UserUrl]
//      val gen = "not supported"
////      MiniUrlGen(mongoUrlPersistence).genUrl()
////      mongoUrlPersistence.TakeMiniUrlById(gen,Bookmark(data.url,"",user=Some(id)))
//      Ok(Json.obj("code" -> gen)).withHeaders(headers: _*)
//    },curUser= Some(id))
//  }
//
//  def unminifyUrl(code : String) = Action { implicit request =>
//    safeAny (request , {
//      logger.info("un minify:" + code)
////      val gen =mongoUrlPersistence.getUrlByMini(code)
////        gen.map(
//
//      Ok(Extraction.decompose(UserUrl("not supported"))).withHeaders(headers: _*)
//    }, auth=false, curUser= None)
//  }
//
//  import utils.TimeChartUtils._
//  def getDashboard(id : String) = Action { implicit request =>
//    safeAny(request, {
//      logger.info("geDashboard")
//      val user = appPersistence.getUser(id, true)
//      user.map { u =>
//        val base = List(
//          generateSeries(u.reffered_users.map(c => c.timestamp), "users"),
//          generateSeries(u.reffered_users_views.map(c => c.timestamp), "user views")
//
//        )
//        val additional =
//          u.providing match {
//            case None =>
//              List(
//                generateSeries(u.pay_referral.map(c => c.timestamp), "payments"),
//                generateSeries(u.reffered_payments_views.map(c => c.timestamp), "pay views"),
//              )
//            case _ =>
//              List(
//                generateSeries(u.pay_received.map(c => c.timestamp), "payments"),
//                generateSeries(u.provider_views.map(c => c.timestamp), "pay views"),
//                generateQualitySeries(u.pay_received.map(c=>c.rate).flatten.toList,"rates")
//
//              )
//          }
//        val chart = base ++ additional
//        val weekago = new DateTime().minusDays(7).getMillis
//        val earnings  = u.pay_received.filter(x=>x.timestamp > weekago).map(y=>y.amount).sum
//        val details = u.providing match {
//          case None => DashboardDetails(
//            u.reffered_users.size,
//            u.reffered_users_views.size,
//            u.pay_referral.size,
//            u.reffered_payments_views.size, chart)
//          case _ =>
//            DashboardDetails(
//              u.reffered_users.size,
//              u.reffered_users_views.size,
//              u.pay_received.size,
//              u.provider_views.size, chart, earnings = Some(earnings),deals = Some(u.pay_received.size))
//        }
//
//        Ok(Extraction.decompose(details)).withHeaders(headers: _*)
//      }.
//        getOrElse(NotFound.withHeaders(headers: _*))
//    },curUser = Some(id))
//
//  }

  def saveUserDetails(id : String) = Action(json4s.json) { implicit request =>
    safe(request, {
      logger.info("sve details called" + request.body.toString)
      val data = request.body.extract[UserUpdate]
      logger.info("UserUpdate extracted")

        appPersistence.updateUser(id, data)
        Ok(Json.obj()).withHeaders(headers: _*)
    },curUser = Some(id))
  }

  def saveUserImage(id : String) = Action(json4s.json) { implicit request =>
    safe(request, {
      logger.info("sve image called" + request.body.toString)
      val data = request.body.extract[ImageUrl]
      logger.info("ImageUrl extracted:" + data)
      appPersistence.updateUserImage(id, data.url)
      Ok(Json.obj()).withHeaders(headers: _*)
    },curUser = Some(id))
  }


  def resetPassword(id : String) = Action(json4s.json) { implicit request =>
    safe(request, {
      logger.info("update password" + request.body.toString)
      val data = request.body.extract[UpdatePassword]
      logger.info("ImageUrl extracted:" + data)

        appPersistence.updatePassword(id, data.password)
        Ok(Json.obj()).withHeaders(headers: _*)
    },auth = false)
  }



//  def pay() = Action(json4s.json) { implicit request =>
//    logger.info("new pay called" + request.body.toString)
//    val data = request.body.extract[Payment]
//    logger.info("paymnet extracted" + data)
//    if ((data.amount == 0) &&
//        (thisUserHasOfferFromThisProvider(data) ) ) {
//        appPersistence.pay(data)
//        Ok(Json.obj()).withHeaders(headers: _*)
//    }else {
//      Forbidden(Json.obj()).withHeaders(headers: _*)
//    }
//    if (data.amount > 0) {
//      payWithStripe(data)
//      logger.info("paying" + data)
//    }
//    appPersistence.pay(data)
//    Ok(Json.obj()).withHeaders(headers: _*)
//  }



  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def headers = List(
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
    "Access-Control-Max-Age" -> "3600",
    "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization, token, otp",
    "Access-Control-Allow-Credentials" -> "true"
  )

  def services = List(
    "AC Service and Repair", "Refrigerator Repair", "Washing Machine Repair", "RO or Water Purifier Repair", "Geyser / Water Heater Repair", "Microwave Repair", "Chimney and Hob Servicing", "TV Repair", "Mobile Repair", "Laptop Repair", "iPhone, iPad, Mac Repair" ,
     "Salon at Home", "Spa at Home for Women", "Party Makeup Artist", "Bridal Makeup Artist", "Pre Bridal Beauty Packages", "Mehendi Artists" ,
      "Carpenter", "Plumber", "Electrician", "Pest Control", "Home Deep Cleaning", "Bathroom Deep Cleaning", "Sofa Cleaning", "Kitchen Deep Cleaning", "Carpet Cleaning", "Geyser / Water Heater Repair", "Washing Machine Repair", "AC Service and Repair", "Microwave Repair", "Refrigerator Repair", "Laptop Repair", "Mobile Repair", "RO or Water Purifier Repair", "TV Repair", "Chimney and Hob Servicing", "iPhone, iPad, Mac Repair" ,
      "CA for Small Business", "Web Designer & Developer", "Packers & Movers", "CA/CS for Company Registration", "CCTV Cameras and Installation", "Graphics Designer", "Lawyer", "Outstation Taxi", "CA for Income Tax Filing", "Visa Agency", "Real Estate Lawyer", "Corporate Event Planner", "GST Registration & Migration Services", "Vastu Shastra Consultants" ,
       "Astrologer", "Baby Portfolio Photographer", "Packers & Movers", "Monthly Tiffin Service", "Passport Agent", "Home Tutor", "Mathematics Tutor", "Commerce Home Tutor", "Outstation Taxi" ,
     "Birthday Party Planner", "Bridal Makeup Artist", "Wedding Planner", "Wedding Photographer", "Party Makeup Artist", "Pre-Wedding Shoot", "Event Photographer", "Mehendi Artists", "Astrologer", "Wedding Choreographe", "Party Caterer", "DJ", "Wedding Caterers", "Corporate Event Planner", "Pre Bridal Beauty Packages"

    )
  def options(p: String) = Action { request =>
    NoContent.withHeaders(headers: _*)
  }
}

object StripeAmountConversionHelper {

  def convert(amount: Double, currencyCode: String): Integer = {
    JBigDecimal.valueOf(amount).movePointRight(resolveDecimalPoints(currencyCode)).intValueExact()
  }

  private def resolveDecimalPoints(currencyCode: String): Int = {
    if (currencyCode.equalsIgnoreCase("MGA"))
      return 0
    else {
      Currency.getInstance(currencyCode.toUpperCase).getDefaultFractionDigits
    }
  }
}

object Fields {
  val amount = "amount"
  val currency = "currency"
  val source = "source"
  val capture = "capture"
  val apiKey = "Stripe.apiKey"
  val card = "card"
  val metadata = "metadata"
  val receiptEmail = "receipt_email"
}
case class CurrencyAmount(val currency : scala.Predef.String, val amount : scala.Double) extends scala.AnyRef with scala.Product with scala.Serializable {
}
case class forTest(a : Option[String])
//object f extends App {
//  implicit val formats = new DefaultFormats {}
//  val x = forTest(Some("1"))
//  println(write(Extraction.decompose(x)))
//  println(write(Extraction.decompose(forTest(None))))
//
//  import com.firebase4s.App
//
//  val serviceAccount = getClass.getResourceAsStream("/ipublishu-198808-firebase-adminsdk-y0a3z-ca8c50a47f.json")
//  App.initialize(serviceAccount, "https://ipublishu-198808.firebaseio.com")
//
//  val db: Database = Database.getInstance()
//  val fooRef: DatabaseReference = db.ref("/notifications")
////  println("before writing")
////  val result =  fooRef.updateChildren(Map("niv3" ->"niv1"))
////  println(Await.result(fooRef.get(),Duration.Inf))
////  println(Await.result(result,Duration.Inf))
////  println("after writing")
//
//  val p = new UserPostgresPersistence("postgres","postgres","postgres","public", None,"52.15.136.250")
//  val users = p.getRST[User]("select data from users")
//  users.foreach { u =>
//    val u1 = p.getUser(u._id,true)
//    val bridge = UUID.randomUUID().toString
//    p.db("users").updateProp(u._id, s"""{"notification_id":"${bridge}"}""")
//    println("before writing")
//    val result =  fooRef.updateChildren(Map(s"$bridge" ->s"${u1.get.notifications_new.size()}"))
//    println("after writing")
//
//  }
//
//}
//
//trait Verifier {
//
//  def verify(idTokenString: String): Boolean
//}
//object GoogleVerifier extends Verifier {
//
//  override def verify(idTokenString: String): Boolean = {
//
//    val clientId = "1056180665798-qaq4v6kcgqeu45hm83mh8kc2a2moa3o1.apps.googleusercontent.com"
//    val transport = GoogleNetHttpTransport.newTrustedTransport
//    val jsonFactory = JacksonFactory.getDefaultInstance
//    val verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
//      .setAudience(ArrayBuffer(clientId)).setIssuer("accounts.google.com").build()
//    val idToken = Option(verifier.verify(idTokenString))
//
//    idToken match {
//      case Some(token) =>
//        println(token.getPayload)
//        true
//
//      case None =>
//        println("Invalid ID token: " + idTokenString)
//        false
//    }
//  }
//}
