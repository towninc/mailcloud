/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.meta.UserDataMeta
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.users.User
import com.google.appengine.api.users.UserServiceFactory
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization

object UserDataService {
  val logger = Logger.getLogger(UserDataService.getClass.getName)

  object UserDataProtocol extends DefaultProtocol {
    import dispatch.json._
    import JsonSerialization._

    implicit object UserDataFormat extends Format[UserData] {
      override def reads(json: JsValue): UserData = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(userData: UserData): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(userData.getKey != null) KeyFactory.keyToString(userData.getKey) else null)),
            (JsString("name"),  tojson(userData.getName)),
            (JsString("email"), tojson(userData.getEmail))
          ))
    }
  }

  def fetchOne( userId:String ):Option[UserData] = {
    val m:UserDataMeta = UserDataMeta.get
    Datastore.query(m).filter(m.userId.equal(userId)).asSingle match {
      case v:UserData => Some(v)
      case null => None
    }
  }

  def fetchByEmail( email:String ):Option[UserData] = {
    val m:UserDataMeta = UserDataMeta.get
    Datastore.query(m).filter(m.email.equal(email)).asSingle match {
      case v:UserData => Some(v)
      case null => None
    }
  }

  def fetchAll():List[UserData] = {
    val m:UserDataMeta = UserDataMeta.get
    Datastore.query(m).asList.toList
  }

  def fetchAllAdmin():List[UserData] = {
    val m:UserDataMeta = UserDataMeta.get
    Datastore.query(m).filter(m.admin.equal(true)).asList.toList
  }

  def createNew():UserData = {
    val result:UserData = new UserData
    result
  }

  def save(model:UserData):Key = {
    val key:Key = model.getKey
    val oldModel:UserData = try {
      Datastore.get(classOf[UserData], key)
    } catch {
      case e:Exception => model
    }
    val isNew:Boolean = (oldModel == model)

    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
    }
    model.setUpdatedAt(now)

    //Unique userId
    var duplicate:Boolean = false;
    if(!Datastore.putUniqueValue("u_uD", model.getUserId())){
      if(!isNew){
        if(oldModel.getUserId != model.getUserId){
          duplicate = true
        }
      } else {
        duplicate = true
      }
    }
    if(duplicate){
      logger.warning("Duplicate user Id. Data has NOT saved")
      throw new DuplicateDataException
    } else {
      Datastore.put(model)
    }
  }

  def getCurrentModel:Option[UserData] = {
    val service = UserServiceFactory.getUserService
    val user:User = service.getCurrentUser
    if ((user != null) && service.isUserAdmin) {
      val id:String = user.getUserId
      val userData:UserData = fetchOne(id) match {
        case Some(v) => v
        case None => {
            val newData:UserData = createNew
            newData.setName(user.getNickname)
            newData.setEmail(user.getEmail)
            newData.setUserId(user.getUserId)
            newData.setCommonSender(false)
            newData.setAdmin(service.isUserAdmin)
            save(newData)
            newData
          }
      }
      Some(userData)
    } else {
      None
    }
  }

  def fetchSenderList(_userData:Option[UserData]):List[UserData] = {
    val result:ListBuffer[UserData] = ListBuffer[UserData]();
    val m:UserDataMeta = UserDataMeta.get
    result.appendAll(Datastore.query(m)
                     .filter(m.commonSender.equal(true)).asList)
    _userData match {
      case Some(userData) => 
        val selfExists:Boolean = result.exists{ e=>
          e.getUserId == userData.getUserId
        }
        
        if(!selfExists) {
          result.append(userData)
        }
      case None =>
    }
    result.toList
  }

  def fetchFirstSender(_userData:Option[UserData]):Option[UserData] = {
    val result:ListBuffer[UserData] = ListBuffer[UserData]();
    val m:UserDataMeta = UserDataMeta.get
    result.appendAll(Datastore.query(m)
                     .filter(m.commonSender.equal(true)).limit(1).asList)
    _userData match {
      case Some(userData) =>
        val selfExists:Boolean = result.exists{ e=>
          e.getUserId == userData.getUserId
        }

        if(!selfExists) {
          result.append(userData)
        }
      case None =>
    }
    if((result != null) && (result.size > 0)){
      Some(result.get(0))
    } else {
      None
    }
  }

  /**
   * get Name - E-mail Pair
   */
  def getSenderPair(data:String):(String, String) = {
    val firstSender = fetchFirstSender(None)
    val (firstSenderName, firstSenderEmail) = firstSender match {
      case Some(f) => (f.getName, f.getEmail)
      case None => ("", "")
    }

    data match {
      case "" => (firstSenderName, firstSenderEmail)
      case null => (firstSenderName, firstSenderEmail)
      case v:String => {
          val name = v.replaceAll("([^<]*) ?<(.*)>", "$1").trim
          val email = v.replaceAll("([^<]*) ?<(.*)>", "$2").trim
          UserDataService.fetchByEmail(email) match {
            case Some(userData) => if(userData.isCommonSender){
                (name, email)
              }  else {
                (firstSenderName, firstSenderEmail)
              }
            case None => (firstSenderName, firstSenderEmail)
          }
        }
    }
  }

  def isUserAdmin:Boolean = {
    val service = UserServiceFactory.getUserService
    val user:User = service.getCurrentUser
    ((user != null) && service.isUserAdmin)
  }

}
