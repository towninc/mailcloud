/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.RecipientMarkMeta
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.MailData
import com.aimluck.model.Recipient
import com.aimluck.model.RecipientGroup
import com.aimluck.model.RecipientMark
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl._
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization

object RecipientService {
  val logger = Logger.getLogger(RecipientService.getClass.getName)

  object RecipientProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object RecipientFormat extends Format[Recipient] {
      override def reads(json: JsValue): Recipient = json match {
        case JsObject(o) =>
          val recipient:Recipient = try {
            val id = fromjson[String](o(JsString(Constants.KEY_ID)))
            RecipientService.fetchOne(id) match {
              case Some(v) => v
              case None => null
            }
          } catch {
            case _ =>
              val model:Recipient = RecipientService.createNew
              model.setEmail(fromjson[String](o(JsString("email"))))
              model.setReplacers(fromjson[List[String]](o(JsString("replacers"))))
              model
          }
          recipient
        case _ => throw new IllegalArgumentException
      }

      def writes(recipient: Recipient): JsValue =
        JsObject(List(
            (JsString(Constants.KEY_ID), tojson(if(recipient.getKey != null) KeyFactory.keyToString(recipient.getKey) else null)),
            (JsString("email"),  tojson(recipient.getEmail)),
            (JsString("replacers"),  tojson(recipient.getReplacers.toList))
          ))
    }
  }
  
  def fetchOne( id:String):Option[Recipient] = {
    val m:RecipientMeta = RecipientMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      Datastore.query(m).filter(m.key.equal(key)).asSingle match {
        case v:Recipient => Some(v)
        case null => None
      }
    } catch {
      case e:Exception => {
          logger.severe(e.getMessage)
          logger.severe(e.getStackTraceString)
          None
        }
    }
  }

  def idListByGroupKey(id:String):List[String] = {
    val key = KeyFactory.stringToKey(id)
    idListByGroupKey(key)
  }

  def idListByGroupKey(key:Key):List[String] = {
    val m:RecipientMeta = RecipientMeta.get
    Datastore.query(m).filter(m.recipientGroupRef.equal(key)).asKeyList.toList.map {
      e:Key => KeyFactory.keyToString(e)
    }
  }

  def createNew():Recipient = {
    val result:Recipient = new Recipient
    result.setEmail("")
    result.setReplacers(List())
    result
  }

  def save(model:Recipient):Key = {
    Datastore.put(model)
  }

  def delete(recipient:Recipient){
    // delete MailDataRecipient
    Datastore.delete(recipient.getKey)
  }

  private def createMarkKey(recipient:Recipient, namespace:String):Key = {
    val m:RecipientMarkMeta = RecipientMarkMeta.get
    KeyFactory.createKey(m.getKind, "%s:%s".format( namespace,
                                                   recipient.getEmail))
  }

  def existsMark(recipient:Recipient, namespace:String):Boolean = {
    val key:Key = createMarkKey(recipient, namespace)
    val old = try {
      Datastore.get(key)
    } catch {
      case _ => null
    }
    (old != null)
  }

  def createMark(recipient:Recipient, namespace:String):RecipientMark = {
    val key:Key = createMarkKey(recipient, namespace)
    val model:RecipientMark = new RecipientMark
    model.setKey(key)
    model.setCreatedAtDay(AppConstants.dayCountFormat.format(new Date))
    model
  }

  def saveMark(recipient:Recipient, namespace:String):Key = {
    Datastore.put(createMark(recipient, namespace))
  }
}
