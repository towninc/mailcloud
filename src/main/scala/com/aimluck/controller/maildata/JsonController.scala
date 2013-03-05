package com.aimluck.controller.maildata;

import com.aimluck.service.MailDataService

import com.aimluck.service.UserDataService
import com.google.appengine.api.blobstore.BlobstoreService
import com.google.appengine.api.blobstore.BlobstoreServiceFactory
import dispatch.json.JsValue
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class JsonController extends AbstractJsonDataController {
  Logger.getLogger(classOf[JsonController].getName)
  
  override def getList:JsValue = {
    import com.aimluck.service.MailDataService.MailDataListProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin) {
      JsonSerialization.tojson(MailDataService.fetchAll(None).sortWith{ (x, y) =>
          x.getUpdatedAt.compareTo(y.getUpdatedAt) > 0
        })
    } else {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getDetail(id:String):JsValue = {
    import com.aimluck.service.MailDataService.MailDataProtocol._
    val startDate:Date =  new Date
    
    if(UserDataService.isUserAdmin) {
      MailDataService.fetchOne(id, None) match {
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
    import com.aimluck.service.MailDataService.MailDataProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin) {
      if((id != null) && (id.size > 0)){
        MailDataService.fetchOne(id, None) match {
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
        tojson(MailDataService.createNew)
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }
}
