/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.DateTimeUtil
import com.aimluck.meta.ContactGroupMeta
import com.aimluck.meta.ContactMeta
import com.aimluck.meta.MailMagazineMeta
import com.aimluck.model.MailMagazine
import com.aimluck.lib.util.AppConstants
import com.aimluck.lib.util.DateTimeUtil
import com.aimluck.model.ContactGroup
import com.aimluck.model.Contact
import com.aimluck.model.ContactGroup
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization

object MailMagazineService {
  val logger = Logger.getLogger(MailMagazineService.getClass.getName)
  val dateTimeFormat:DateFormat = MailMagazineService.dateTimeFormatter;
  val dateFormat:DateFormat = MailMagazineService.dateFormatter;

  object MailMagazineProtocol extends DefaultProtocol {
    import dispatch.json._
    import JsonSerialization._

    implicit object MailMagazineFormat extends Format[MailMagazine] {
      override def reads(json: JsValue): MailMagazine = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(mailMagazine: MailMagazine): JsValue = {
        val (senderName:String, senderEmail:String) =
          UserDataService.getSenderPair(mailMagazine.getSender)
        
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(mailMagazine.getKey != null) KeyFactory.keyToString(mailMagazine.getKey) else null)),
            (JsString("contactGroupKey"), tojson(if(mailMagazine.getContactGroupRef.getKey != null) KeyFactory.keyToString(mailMagazine.getContactGroupRef.getKey) else null)),
            (JsString("subject"),  tojson(mailMagazine.getSubject)),
            (JsString("content"),  tojson(mailMagazine.getContent)),
            (JsString("sender"),  tojson(mailMagazine.getSender)),
            (JsString("sendDateTime"),  tojson(if(mailMagazine.getSendDateTime != null) AppConstants.dateTimeFormat.format(mailMagazine.getSendDateTime) else null)),
            (JsString("createdAt"), tojson(if(mailMagazine.getCreatedAt != null) dateTimeFormat.format(mailMagazine.getCreatedAt) else null)),
            (JsString("isReplaced"),  tojson(mailMagazine.isReplaced)),
            (JsString("senderName"), tojson(senderName)),
            (JsString("senderEmail"), tojson(senderEmail)),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("mailMagazine"), mailMagazine.getSubject)))))
          ))
      }
    }
  }

  object MailMagazineListProtocol extends DefaultProtocol {
    import dispatch.json._
    import JsonSerialization._

    implicit object MailMagazineFormat extends Format[MailMagazine] {
      override def reads(json: JsValue): MailMagazine = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(mailMagazine: MailMagazine): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(mailMagazine.getKey != null) KeyFactory.keyToString(mailMagazine.getKey) else null)),
            (JsString("contactGroupKey"), tojson(if(mailMagazine.getContactGroupRef.getKey != null) KeyFactory.keyToString(mailMagazine.getContactGroupRef.getKey) else null)),
            (JsString("subject"),  tojson(mailMagazine.getSubject)),
            (JsString("sender"),  tojson(mailMagazine.getSender)),
            (JsString("sendDateTime"),  tojson(if(mailMagazine.getSendDateTime != null) AppConstants.dateTimeFormat.format(mailMagazine.getSendDateTime) else AppConstants.DEFAULT_STEPMAIL_SENDTIME)),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("mailMagazine"), mailMagazine.getSubject)))))
          ))
    }
  }

  def fetchOne( id:String, _userData:Option[UserData] ):Option[MailMagazine] = {
    val m:MailMagazineMeta = MailMagazineMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      _userData match {
        case Some(userData) =>{
            Datastore.query(m).filter(m.key.equal(key))
            .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
              case v:MailMagazine => Some(v)
              case null => None
            }
          }
        case None => {
            Datastore.query(m).filter(m.key.equal(key)).asSingle match {
              case v:MailMagazine => Some(v)
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

  def fetchAll(contactGroup:ContactGroup):List[MailMagazine] = {
    val m:MailMagazineMeta = MailMagazineMeta.get
    Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).asList.toList
  }

  def count(contactGroup:ContactGroup):Int = {
    val m:MailMagazineMeta = MailMagazineMeta.get
    Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).count()
  }

  def createNew(contactGroup:ContactGroup):MailMagazine = {
    val result:MailMagazine = new MailMagazine
    result.setSubject("")
    result.setContent("")
    setContactGroup(result, contactGroup)
    result.setReplaced(true)
    result.setActive(false)
    result.setSendDateTime(DateTimeUtil.newSendDateTime(new Date))
    result
  }

  def saveWithUserData(model:MailMagazine, userData:UserData):Key = {
    val key:Key = model.getKey

    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
      model.setLastSentAt(
        DateTimeUtil.getOneDayBefore(now)
      )
    }

    model.setUpdatedAt(now)
    model.getUserDataRef.setModel(userData)
    Datastore.put(userData, model).apply(1)
  }

  def delete(mailMagazine:MailMagazine):Unit = {
    Datastore.delete(mailMagazine.getKey)
  }

  def setContactGroup(mailMagazine:MailMagazine, contactGroup:ContactGroup):Unit = {
    mailMagazine.getContactGroupRef.setModel(contactGroup)
    mailMagazine.setTimeZone(contactGroup.getTimeZone)
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

  def fetchToSendByDate(date:Date):List[MailMagazine] = {
    val m:MailMagazineMeta = MailMagazineMeta.get
    Datastore.query(m).filter(m.active.equal(true))
    .asList.filter{
      e => {
        val modelTimeZone = try{
          TimeZone.getTimeZone(e.getTimeZone)
        } catch {
          case _ => AppConstants.timeZone
        }

        val dateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(date)
        val sendDateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(e.getSendDateTime)
        val lastSentDateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(e.getLastSentAt)

        val dateTimeCalendar:Calendar = Calendar.getInstance(modelTimeZone)
        dateTimeCalendar.setTime(date)
        val startTimeCalendar:Calendar = Calendar.getInstance(modelTimeZone)
        startTimeCalendar.setTime(e.getSendDateTime)
        
        (
          (sendDateStr == dateStr)
          && (startTimeCalendar.getTimeInMillis <= dateTimeCalendar.getTimeInMillis)
          && (e.getContactGroupRef.getModel.isBusy == false)
        )
      }
    }.toList
  }
}
