/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.ContactMarkMeta
import com.aimluck.meta.ContactMeta
import com.aimluck.model.Contact
import com.aimluck.model.ContactGroup
import com.aimluck.model.ContactMark
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.users.User
import com.google.appengine.api.users.UserServiceFactory
import java.util.Date
import java.util.TimeZone
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.slim3.datastore.Datastore
import org.slim3.datastore.ModelQuery
import org.slim3.datastore.S3QueryResultList
import scala.collection.JavaConversions._
import sjson.json.DefaultProtocol
import sjson.json.DefaultProtocol._
import sjson.json.Format
import sjson.json.JsonSerialization

object ContactService {
  val logger = Logger.getLogger(ContactService.getClass.getName)

  object ContactProtocol extends DefaultProtocol {
    import dispatch.json._
    import JsonSerialization._

    implicit object ContactFormat extends Format[Contact] {
      override def reads(json: JsValue): Contact = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(contact: Contact): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), if(contact.getKey != null) tojson(KeyFactory.keyToString(contact.getKey)) else null),
            (JsString("name"),  tojson(contact.getName)),
            (JsString("email"),  tojson(contact.getEmail)),
            (JsString("isSelf"), tojson(contact.isSelf)),
            (JsString("replacers"), tojson(if(contact.getReplacers != null) contact.getReplacers.toList else List())),
            (JsString("createdAt"), if(contact.getCreatedAt != null) tojson(AppConstants.dateTimeFormat.format(contact.getCreatedAt)) else tojson("")),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("contact"), contact.getEmail)))))
          ))
    }
  }

  def fetchOne( id:String, _userData:Option[UserData] ):Option[Contact] = {
    val m:ContactMeta = ContactMeta.get
    val key = KeyFactory.stringToKey(id)
    _userData match {
      case Some(userData) =>{
          Datastore.query(m).filter(m.key.equal(key))
          .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
            case v:Contact => Some(v)
            case null => None
          }
        }
      case None => {
          Datastore.query(m).filter(m.key.equal(key)).asSingle match {
            case v:Contact => Some(v)
            case null => None
          }
        }
    }
  }

  def fetchResultList(contactGroup:ContactGroup, _cursor:Option[String]):S3QueryResultList[Contact] = {
    val m:ContactMeta = ContactMeta.get
    //cursor.map
    val query:ModelQuery[Contact] = Datastore.query(m)
    .filter(m.contactGroupRef.equal(contactGroup.getKey))
    .limit(AppConstants.RESULTS_PER_PAGE);

    _cursor match {
      case Some(cursor) =>
        query.encodedStartCursor(cursor).asQueryResultList()
      case None => query.asQueryResultList()
    }
  }

  def fetchFirst(contactGroup:ContactGroup):Contact = {
    val m:ContactMeta = ContactMeta.get
    val list:List[Contact] =
      Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).limit(1).asList.toList
    if((list != null) && (list.size > 0)){
      list.get(0)
    } else {
      null
    }
  }

  def createNew():Contact = {
    val result:Contact = new Contact
    result.setName("")
    result.setEmail("")
    result.setSelf(false)
    result.setReplacers(List())
    result
  }

  def setCreatedAt(model:Contact, date:Date):Unit = {
    val timeZone:TimeZone = try{
      TimeZone.getTimeZone(model.getTimeZone)
    } catch {
      case _ => AppConstants.timeZone
    }
    model.setTimeZone(timeZone.getID)
    model.setCreatedAt(date)
    model.setCreatedAtDay(AppConstants.dayCountFormat.format(date))
    model.setCreatedAtDayWithTimeZone(AppConstants.dayCountFormatWithTimeZone(timeZone).format(date))
  }

  def saveWithGroup(model:Contact, contactGroup:ContactGroup):Key = {
    val key:Key = model.getKey
    val oldModel:Contact = try {
      Datastore.get(classOf[Contact], key)
    } catch {
      case e:Exception => model
    }
    val isNew:Boolean = (oldModel == model)
    if(isNew){
      val mark = fetchMark(model.getEmail, contactGroup)
      if(mark != null){
        logger.warning("Duplicate email for user. Data has NOT saved")
        throw new DuplicateDataException
      }
      saveMark(model, contactGroup)
    }

    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setTimeZone(contactGroup.getTimeZone)
      setCreatedAt(model, now)
    }

    if(model.isSelf){
      val user:User = UserServiceFactory.getUserService.getCurrentUser
      if (user != null) {
        model.setEmail(user.getEmail)
      }
    }

    model.setUpdatedAt(now)
    model.getContactGroupRef.setModel(contactGroup)
    model.getUserDataRef.setModel(contactGroup.getUserDataRef.getModel)
    model.setEmailGroupKey

    Datastore.put(model)
  }

  def delete(contact:Contact){
    // delete ReminderContact
    if(!contact.isSelf){
      Datastore.delete(createMarkKey(contact.getEmail, contact.getContactGroupRef.getModel))
      Datastore.delete(contact.getKey)
    }
  }


  private def createMarkKey(email:String, contactGroup:ContactGroup):Key = {
    val m:ContactMarkMeta = ContactMarkMeta.get
    KeyFactory.createKey(m.getKind, "%s:%s".format( KeyFactory.keyToString(contactGroup.getKey),
                                                   email))
  }

  def fetchMark(email:String, contactGroup:ContactGroup):ContactMark = {
    val key:Key = createMarkKey(email, contactGroup)
    val mark:ContactMark = try {
      Datastore.get(classOf[ContactMark], key)
    } catch {
      case _ => null
    }
    mark
  }

  def createMark(contact:Contact, contactGroup:ContactGroup):ContactMark = {
    val key:Key = createMarkKey(contact.getEmail, contactGroup)
    val model:ContactMark = new ContactMark
    model.setKey(key)
    model.getContactRef.setModel(contact)
    model
  }

  def saveMark(contact:Contact, contactGroup:ContactGroup):Key = {
    Datastore.put(createMark(contact, contactGroup))
  }
}
