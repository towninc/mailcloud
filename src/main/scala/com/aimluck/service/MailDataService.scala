/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.meta.MailDataErrorMarkMeta
import com.aimluck.meta.MailDataMarkMeta
import com.aimluck.meta.MailDataMeta
import com.aimluck.meta.RecipientGroupMeta
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.MailData
import com.aimluck.lib.util.AppConstants
import com.aimluck.model.MailDataErrorMark
import com.aimluck.model.MailDataMark
import com.aimluck.model.Recipient
import com.aimluck.model.RecipientGroup
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.logging.Logger
import org.dotme.liquidtpl.helper.BasicHelper
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization

object MailDataService {
  val logger = Logger.getLogger(MailDataService.getClass.getName)
  val dateTimeFormat:DateFormat = MailDataService.dateTimeFormatter;
  val dateFormat:DateFormat = MailDataService.dateFormatter;

  object Status extends Enumeration {
    val INITIALIZING = Value("I")
    val REGISTERED = Value("R")
    val SENDING = Value("S")
    val ERROR = Value("E")
    val FINISHED = Value("F")
  }

  object MailDataProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object MailDataFormat extends Format[MailData] {
      override def reads(json: JsValue): MailData = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(mailData: MailData): JsValue = {
        val (senderName:String, senderEmail:String) =
          UserDataService.getSenderPair(mailData.getSender)
        
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(mailData.getKey != null) KeyFactory.keyToString(mailData.getKey) else null)),
            (JsString("subject"),  tojson(mailData.getSubject)),
            (JsString("content"),  tojson(mailData.getContent)),
            (JsString("sender"),  tojson(mailData.getSender)),
            (JsString("isReplaced"),  tojson(mailData.isReplaced)),
            (JsString("status"), tojson(mailData.getStatus)),
            (JsString("statusString"), tojson(statusString(mailData))),
            (JsString("statusMap"), BasicHelper.jsonFromStringPairs(statusMap)),
            (JsString("senderName"), tojson(senderName)),
            (JsString("senderEmail"), tojson(senderEmail)),
            (JsString("sentAt"), if((mailData.getStatus == Status.FINISHED.toString) && (mailData.getSentAt != null)) tojson(AppConstants.dateTimeFormat.format(mailData.getSentAt)) else tojson("")),
            (JsString("createdAt"), if(mailData.getCreatedAt != null) tojson(AppConstants.dateTimeFormat.format(mailData.getCreatedAt)) else tojson(""))
          ))
      }
    }
  }

  object MailDataListProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object MailDataFormat extends Format[MailData] {
      override def reads(json: JsValue): MailData = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(mailData: MailData): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(mailData.getKey != null) KeyFactory.keyToString(mailData.getKey) else null)),
            (JsString("subject"),  tojson(mailData.getSubject)),
            (JsString("sender"),  tojson(mailData.getSender)),
            (JsString("statusString"), tojson(statusString(mailData))),
            (JsString("sentAt"), if((mailData.getStatus == Status.FINISHED.toString) && (mailData.getSentAt != null)) tojson(AppConstants.dateTimeFormat.format(mailData.getSentAt)) else tojson("")),
            (JsString("createdAt"), if(mailData.getCreatedAt != null) tojson(AppConstants.dateTimeFormat.format(mailData.getCreatedAt)) else tojson(""))
          ))
    }
  }

  def fetchOne( id:String, _userData:Option[UserData] ):Option[MailData] = {
    val m:MailDataMeta = MailDataMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      _userData match {
        case Some(userData) =>{
            Datastore.query(m).filter(m.key.equal(key))
            .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
              case v:MailData => Some(v)
              case null => None
            }
          }
        case None => {
            Datastore.query(m).filter(m.key.equal(key)).asSingle match {
              case v:MailData => Some(v)
              case null => None
            }
          }
      }
      
    } catch {
      case e:Exception => {
          logger.severe(e.getMessage)
          logger.severe(e.getStackTraceString)
          None
        }
    }
  }

  def fetchAll(_userData:Option[UserData]):List[MailData] = {
    val m:MailDataMeta = MailDataMeta.get
    _userData match {
      case Some(userData) => userData.getMailDataRef.getModelList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def createNew():MailData = {
    val result:MailData = new MailData
    result.setSubject("")
    result.setContent("")
    result.setSender("")
    result.setStatus(Status.INITIALIZING.toString)
    result.setRecipientCountAll(0)
    result.setSentCount(0)
    result.setReplaced(true)
    result
  }

  def saveWithUserData(model:MailData, userData:UserData):Key = {
    val key:Key = model.getKey

    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
    }
    model.setUpdatedAt(now)
    model.getUserDataRef.setModel(userData)
    Datastore.put(userData, model).apply(1)
  }

  def cleanRecipients(mailData:MailData):Unit = {
    if((mailData.getStatus == MailDataService.Status.FINISHED.toString)
       || (mailData.getStatus == MailDataService.Status.ERROR.toString)) {
      val rGM:RecipientGroupMeta = RecipientGroupMeta.get
      val rM:RecipientMeta = RecipientMeta.get
      Datastore.query(rGM)
      .filter(rGM.mailDataRef.equal(mailData.getKey)).asKeyList.foreach{ key =>
        Datastore.delete(Datastore.query(rM).filter(rM.recipientGroupRef.equal(key)).asKeyList)
        Datastore.delete(key)
      }
    }
  }

  def delete(mailData:MailData){
    Datastore.delete(mailData.getKey)
  }

  val statusMap:List[(String, String)] = List[(String, String)](
    Status.INITIALIZING.toString -> LanguageUtil.get("mailData.Status.initializing"),
    Status.REGISTERED.toString -> LanguageUtil.get("mailData.Status.registered"),
    Status.SENDING.toString -> LanguageUtil.get("mailData.Status.sending"),
    Status.FINISHED.toString -> LanguageUtil.get("mailData.Status.finished"),
    Status.ERROR.toString -> LanguageUtil.get("mailData.Status.error")
  )

  def fetchToSendByDate(date:Date, timeZone:TimeZone):List[MailData] = {
    val toSendDateCal = Calendar.getInstance( AppConstants.timeZone )
    toSendDateCal.setTime(date)
    toSendDateCal.set(Calendar.HOUR_OF_DAY, 0)
    toSendDateCal.set(Calendar.MINUTE, 0)
    toSendDateCal.set(Calendar.SECOND, 0)
    toSendDateCal.set(Calendar.MILLISECOND, 0)

    val sentAtDateCal = Calendar.getInstance( AppConstants.timeZone )
    val toSendDateTimeCal = Calendar.getInstance( AppConstants.timeZone )

    val m:MailDataMeta = MailDataMeta.get
    val list:List[MailData] = Datastore.query(m)
    .filter(m.status.equal(Status.REGISTERED.toString)).asList.toList
    list
  }

  def fetchToSendByDate(date:Date):List[MailData] = {
    fetchToSendByDate(date, AppConstants.timeZone)
  }

  def dateTimeFormatter(timezone:Option[TimeZone]):DateFormat = {
    val dateTimeFormat:DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm")
    timezone match {
      case Some(v) => dateTimeFormat.setTimeZone(v)
      case None=> dateTimeFormat.setTimeZone(AppConstants.timeZone)
    }
    dateTimeFormat
  }

  def dateTimeFormatter():DateFormat = {
    dateTimeFormatter(None)
  }

  def dateFormatter(timezone:Option[TimeZone]):DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("yyyy/MM/dd")
    timezone match {
      case Some(v) => dateFormat.setTimeZone(v)
      case None=> dateFormat.setTimeZone(AppConstants.timeZone)
    }
    dateFormat
  }

  def dateFormatter():DateFormat = {
    dateFormatter(None)
  }

  def replaceString(string:String, list:List[String]):String = {
    var count = 0;
    var result = string;
    for( e <- 'A' to 'Z') {
      if(list.size > count){
        result = result.replaceAll("([^\\$]|^)\\$%s".format(e.toString), 
                                   "$1%s".format(list.get(count)))
      } else {
        result = result.replaceAll("([^\\$]|^)\\$%s".format(e.toString), "$1")
      }
      count += 1
    }
    result.replaceAll("\\$\\$", "\\$")
  }
  
  def replacedMap(mailData:MailData, recipient:Recipient):Map[String, String] = {
    if(mailData.isReplaced){
      Map(
        ("subject" -> replaceString(mailData.getSubject, recipient.getReplacers.toList)),
        ("content" -> replaceString(mailData.getContent, recipient.getReplacers.toList))
      )
    } else {
      Map(
        ("subject" -> mailData.getSubject),
        ("content" -> mailData.getContent)
      )
    }
  }

  def statusString(mailData:MailData):String = {
    val pair:(String, String) = statusMap
    .find(p => (p._1 == mailData.getStatus)).getOrElse(null)

    var recipientsAll:Long = mailData.getRecipientCountAll.longValue
    var registered:Long = 0
    if(mailData.getKey != null){
      RecipientCsvService.fetchOne(mailData) match {
        case Some(recipientCsv) =>
          mailData.getRecipientGroupRef.getModelList.foreach{ rG =>
            registered += rG.getRecipientCount.longValue
          }
        case None =>
      }
    }

    pair match {
      case (u, v) => v
        if(u == Status.INITIALIZING.toString) {
          if( recipientsAll > 0 ) {
            "%s (%d/%d)".format(v, registered , recipientsAll)
          } else {
            v
          }
        } else if(u == Status.SENDING.toString) {
          "%s (%d/%d)".format(v, mailData.getSentCount, recipientsAll)
        } else if(u == Status.FINISHED.toString){
          "%s (%d)".format(v, mailData.getSentCount)
        }else {
          v
        }
      case null => ""
    }
  }

  private def createMarkKey(mailData:MailData, recipient:Recipient):Key = {
    val m:MailDataMarkMeta = MailDataMarkMeta.get
    KeyFactory.createKey(m.getKind, "%s:%s".format(
        KeyFactory.keyToString(recipient.getKey),
        recipient.getEmail
      ))
  }

  def existsMark(mailData:MailData, recipient:Recipient):Boolean = {
    val key:Key = createMarkKey(mailData, recipient)
    val old = try {
      Datastore.get(key)
    } catch {
      case _ => null
    }
    (old != null)
  }

  def createMark(mailData:MailData, recipient:Recipient, mailType:String):MailDataMark = {
    val now:Date = new Date
    val key:Key = createMarkKey(mailData, recipient)
    val model:MailDataMark = new MailDataMark
    model.setKey(key)
    model.setEmail(recipient.getEmail)
    model.setSentAt(now)
    model.setSentAtDay(AppConstants.dayCountFormat.format(now))
    model.getMailDataRef.setKey(mailData.getKey)
    model.setMailType(mailType)
    model.getRecipientGroupRef.setKey(recipient.getRecipientGroupRef.getKey)
    model
  }

  def saveMark(mailData:MailData, recipient:Recipient, mailType:String):Key = {
    Datastore.put(createMark(mailData, recipient, mailType))
  }

  private def createErrorMarkKey(mailData:MailData, recipient:Recipient):Key = {
    val m:MailDataErrorMarkMeta = MailDataErrorMarkMeta.get
    KeyFactory.createKey(m.getKind, "%s:%s".format(
        KeyFactory.keyToString(recipient.getKey),
        recipient.getEmail
      ))
  }

  def existsErrorMark(mailData:MailData, recipient:Recipient):Boolean = {
    val key:Key = createErrorMarkKey(mailData, recipient)
    val old = try {
      Datastore.get(key)
    } catch {
      case _ => null
    }
    (old != null)
  }

  def createErrorMark(mailData:MailData, recipient:Recipient, mailType:String):MailDataErrorMark = {
    val now:Date = new Date
    val key:Key = createErrorMarkKey(mailData, recipient)
    val model:MailDataErrorMark = new MailDataErrorMark
    model.setKey(key)
    model.setEmail(recipient.getEmail)
    model.setErrorAt(now)
    model.setErrorAtDay(AppConstants.dayCountFormat.format(now))
    model.getMailDataRef.setKey(mailData.getKey)
    model.setMailType(mailType)
    model.getRecipientGroupRef.setKey(recipient.getRecipientGroupRef.getKey)
    model
  }

  def saveErrorMark(mailData:MailData, recipient:Recipient, mailType:String, message:String):Key = {
    val key:Key = createErrorMarkKey(mailData, recipient)
    val mark = try {
      val temp = Datastore.get(classOf[MailDataErrorMark], key)
      val now:Date = new Date
      temp.setErrorAt(now)
      temp.setErrorAtDay(AppConstants.dayCountFormat.format(now))
      temp
    } catch {
      case _ => createErrorMark(mailData, recipient, mailType)
    }
    mark.setMessage(message);
    Datastore.put(mark)
  }

  def deleteErrorMark(mailData:MailData, recipient:Recipient):Unit = {
    val key:Key = createErrorMarkKey(mailData, recipient)
    Datastore.delete(key)
  }
}
