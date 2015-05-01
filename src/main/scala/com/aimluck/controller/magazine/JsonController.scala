package com.aimluck.controller.magazine;

import com.aimluck.service.MailMagazineService

import com.aimluck.service.UserDataService
import com.aimluck.model.ContactGroup
import com.aimluck.service.ContactGroupService
import com.google.appengine.api.datastore.KeyFactory
import dispatch.classic.json.JsObject
import dispatch.classic.json.JsString
import dispatch.classic.json.JsValue
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class JsonController extends AbstractJsonDataController {
  Logger.getLogger(classOf[JsonController].getName)
  
  override def getList:JsValue = {
    import com.aimluck.service.MailMagazineService.MailMagazineListProtocol._
    val contactGroupKey = request.getParameter("contactGroupKey")
    try {
      KeyFactory.stringToKey(contactGroupKey)
    } catch {
      case _ => {
          val redirectUri:String = ContactGroupService.fetchFirst(None) match {
            case null => "/contact/nogroup"
            case first:ContactGroup => "/magazine/index?contactGroupKey=%s"
              .format(KeyFactory.keyToString(first.getKey))
          }
          return JsObject(List(
              (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_SUCCESS)),
              (JsString(Constants.KEY_REDIRECT), tojson(redirectUri))
            ))
        }
    }

    if(UserDataService.isUserAdmin) {
      ContactGroupService.fetchOne(contactGroupKey, None) match {
        case Some(contactGroup) => JsonSerialization.tojson(
            MailMagazineService.fetchAll(contactGroup).sortWith{
              (x, y) => x.getSendDateTime.getTime < y.getSendDateTime.getTime
            }
          )
        case None => {
            addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
            null
          }
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getDetail(id:String):JsValue = {
    import com.aimluck.service.MailMagazineService.MailMagazineProtocol._
    if(UserDataService.isUserAdmin) {
      MailMagazineService.fetchOne(id, None) match {
        case Some(v) => {
            tojson(v)
          }
        case None => {
            addError(Constants.KEY_GLOBAL_ERROR,
                     LanguageUtil.get("error.dataNotFound"))
            null
          }
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getForm(id:String):JsValue = {
    import com.aimluck.service.MailMagazineService.MailMagazineProtocol._
    val contactGroupKey = request.getParameter("contactGroupKey")
    try {
      KeyFactory.stringToKey(contactGroupKey)
    } catch {
      case _ => {
          val redirectUri:String = ContactGroupService.fetchFirst(None) match {
            case null => "/contact/nogroup"
            case first:ContactGroup => "/magazine/form?contactGroupKey=%s"
              .format(KeyFactory.keyToString(first.getKey))
          }
          return JsObject(List(
              (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_SUCCESS)),
              (JsString(Constants.KEY_REDIRECT), tojson(redirectUri))
            ))
        }
    }
    
    if(UserDataService.isUserAdmin) {
      ContactGroupService.fetchOne(contactGroupKey, None) match {
        case Some(contactGroup) =>
          if((id != null) && (id.size > 0)){
            MailMagazineService.fetchOne(id, None) match {
              case Some(v) => {
                  tojson(v)
                }
              case None => {
                  addError(Constants.KEY_GLOBAL_ERROR,
                           LanguageUtil.get("error.dataNotFound"))
                  null
                }
            }
          } else {
            tojson(MailMagazineService.createNew(contactGroup))
          }
        case None => addError(Constants.KEY_GLOBAL_ERROR,
                              LanguageUtil.get("error.dataNotFound"))
          null
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }
}
