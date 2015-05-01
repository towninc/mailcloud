/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.DateTimeUtil
import com.aimluck.meta.ContactGroupMeta
import com.aimluck.meta.ContactMeta
import com.aimluck.meta.StepMailMeta
import com.aimluck.model.StepMail
import com.aimluck.lib.util.AppConstants
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

object StepMailService {
  val logger = Logger.getLogger(StepMailService.getClass.getName)
  val dateTimeFormat:DateFormat = StepMailService.dateTimeFormatter;
  val dateFormat:DateFormat = StepMailService.dateFormatter;

  object StepMailProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object StepMailFormat extends Format[StepMail] {
      override def reads(json: JsValue): StepMail = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(stepMail: StepMail): JsValue = {
        val (senderName:String, senderEmail:String) =
          UserDataService.getSenderPair(stepMail.getSender)
        
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(stepMail.getKey != null) KeyFactory.keyToString(stepMail.getKey) else null)),
            (JsString("contactGroupKey"), tojson(if(stepMail.getContactGroupRef.getKey != null) KeyFactory.keyToString(stepMail.getContactGroupRef.getKey) else null)),
            (JsString("subject"),  tojson(stepMail.getSubject)),
            (JsString("content"),  tojson(stepMail.getContent)),
            (JsString("sender"),  tojson(stepMail.getSender)),
            (JsString("sendTime"),  tojson(if(stepMail.getSendTime != null) AppConstants.timeFormat.format(stepMail.getSendTime) else AppConstants.DEFAULT_STEPMAIL_SENDTIME)),
            (JsString("createdAt"), tojson(if(stepMail.getCreatedAt != null) dateTimeFormat.format(stepMail.getCreatedAt) else null)),
            (JsString("intervalDays"), tojson(stepMail.getIntervalDays.toString)),
            (JsString("isReplaced"),  tojson(stepMail.isReplaced)),
            (JsString("senderName"), tojson(senderName)),
            (JsString("senderEmail"), tojson(senderEmail)),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("stepMail"), stepMail.getSubject)))))
          ))
      }
    }
  }

  object StepMailListProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object StepMailFormat extends Format[StepMail] {
      override def reads(json: JsValue): StepMail = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(stepMail: StepMail): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(stepMail.getKey != null) KeyFactory.keyToString(stepMail.getKey) else null)),
            (JsString("contactGroupKey"), tojson(if(stepMail.getContactGroupRef.getKey != null) KeyFactory.keyToString(stepMail.getContactGroupRef.getKey) else null)),
            (JsString("subject"),  tojson(stepMail.getSubject)),
            (JsString("sender"),  tojson(stepMail.getSender)),
            (JsString("intervalDays"), tojson(stepMail.getIntervalDays.toString)),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("stepMail"), stepMail.getSubject)))))
          ))
    }
  }

  def fetchOne( id:String, _userData:Option[UserData] ):Option[StepMail] = {
    val m:StepMailMeta = StepMailMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      _userData match {
        case Some(userData) =>{
            Datastore.query(m).filter(m.key.equal(key))
            .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
              case v:StepMail => Some(v)
              case null => None
            }
          }
        case None => {
            Datastore.query(m).filter(m.key.equal(key)).asSingle match {
              case v:StepMail => Some(v)
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

  def fetchAll(contactGroup:ContactGroup):List[StepMail] = {
    val m:StepMailMeta = StepMailMeta.get
    Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).asList.toList
  }

  def count(contactGroup:ContactGroup):Int = {
    val m:StepMailMeta = StepMailMeta.get
    Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).count()
  }

  def createNew(contactGroup:ContactGroup):StepMail = {
    val result:StepMail = new StepMail
    result.setSubject("")
    result.setContent("")
    setContactGroup(result, contactGroup)
    result.setIntervalDays(1)
    result.setReplaced(true)
    result
  }

  def saveWithUserData(model:StepMail, userData:UserData):Key = {
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

  def delete(stepMail:StepMail){
    Datastore.delete(stepMail.getKey)
  }

  def setContactGroup(stepMail:StepMail, contactGroup:ContactGroup):Unit = {
    stepMail.getContactGroupRef.setModel(contactGroup)
    stepMail.setTimeZone(contactGroup.getTimeZone)
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

  def fetchToSendByDate(date:Date):List[StepMail] = {
    val m:StepMailMeta = StepMailMeta.get
    Datastore.query(m)
    .asList.filter{ e =>
      val modelTimeZone = try{
        TimeZone.getTimeZone(e.getTimeZone)
      } catch {
        case _ => AppConstants.timeZone
      }

      val dateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(date)
      val lastSentDateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(e.getLastSentAt)

      val dateTimeCalendar:Calendar = Calendar.getInstance(modelTimeZone)
      dateTimeCalendar.setTime(date)
      val startTimeCalendar:Calendar = Calendar.getInstance(modelTimeZone)
      startTimeCalendar.setTime(e.getSendTime)
      startTimeCalendar.set(Calendar.YEAR, dateTimeCalendar.get(Calendar.YEAR))
      startTimeCalendar.set(Calendar.MONTH, dateTimeCalendar.get(Calendar.MONTH))
      startTimeCalendar.set(Calendar.DATE, dateTimeCalendar.get(Calendar.DATE))
        
      (
        (dateStr.toInt > lastSentDateStr.toInt)
        && (startTimeCalendar.getTimeInMillis <= dateTimeCalendar.getTimeInMillis)
        && (e.getContactGroupRef.getModel.isBusy == false)
      )
    }.toList
  }
}
