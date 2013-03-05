/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.RecipientGroupMeta
import com.aimluck.model.MailData
import com.aimluck.model.RecipientGroup
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl._
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import sjson.json.DefaultProtocol._
import sjson.json.JsonSerialization._

object RecipientGroupService {
  val logger = Logger.getLogger(RecipientGroupService.getClass.getName)

  def fetchOne( id:String):Option[RecipientGroup] = {
    val m:RecipientGroupMeta = RecipientGroupMeta.get
    val key = KeyFactory.stringToKey(id)
    Datastore.query(m).filter(m.key.equal(key)).asSingle match {
      case v:RecipientGroup => Some(v)
      case null => None
    }
  }

  def fetchAll(_userData:Option[UserData]):List[RecipientGroup] = {
    val m:RecipientGroupMeta = RecipientGroupMeta.get
    _userData match {
      case Some(userData) => Datastore.query(m).filter(m.userDataRef.equal(userData.getKey)).asList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def createNew():RecipientGroup = {
    val result:RecipientGroup = new RecipientGroup
    result.setRecipientCount(0)
    result
  }

  def delete(recipientGroup:RecipientGroup){
    Datastore.deleteAll(recipientGroup.getKey)
  }

  def countRecipientsByDate(date:Date):Long = {
    val m:RecipientGroupMeta = RecipientGroupMeta.get
    var count:Long = 0
    Datastore.query(m)
    .filter(m.createdAtDay.equal(AppConstants.dayCountFormat.format(date)))
    .asList.map{ _.getRecipientCount }
    .foreach{c => count += c.longValue}
    count
  }

  def saveWithUserData(model:RecipientGroup, userData:UserData):Key = {
    saveWithMailData(model, userData, None)
  }

  def saveWithMailData(model:RecipientGroup, mailData:MailData):Key = {
    saveWithMailData(model, mailData.getUserDataRef.getModel, Some(mailData))
  }

  def saveWithMailData(model:RecipientGroup, userData:UserData, _mailData:Option[MailData]):Key = {
    val key:Key = model.getKey
    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
    }
    model.getUserDataRef.setModel(userData)
    _mailData match {
      case Some(mailData) => {
          model.getMailDataRef.setModel(mailData)
          Datastore.put(userData, mailData, model).apply(2)
        }
      case None => Datastore.put(userData, model).apply(1)
    }
  }

  def countRecipientsToday():Long = {
    countRecipientsByDate(new Date)
  }
}
