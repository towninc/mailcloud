package com.aimluck.controller.userdata;


import com.aimluck.model.UserData
import com.aimluck.service.UserDataService
import dispatch.classic.json.JsString
import dispatch.classic.json.JsValue
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class JsonController extends AbstractJsonDataController {
  Logger.getLogger(classOf[JsonController].getName)
  val KEY_SENDER = "sender"
  val SENDER_WITH_LOGIN = "withlogin"
  val SENDER_WITHOUT_LOGIN = "withoutlogin"


  override def getList:JsValue = {
    import com.aimluck.service.UserDataService.UserDataProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin) {
      UserDataService.getCurrentModel match{
        case Some(userData) => {
            putExtraInformation("assignCheck", LanguageUtil.get("assignCheck", Some(Array(userData.getEmail))))
            if(userData.isCommonSender){
              putExtraInformation("assignFlag", "true")
            }
        }
        case None =>
      }
      request.getAttribute(KEY_SENDER) match {
        case SENDER_WITH_LOGIN => JsonSerialization.tojson(
            UserDataService.fetchSenderList(UserDataService.getCurrentModel))
        case SENDER_WITHOUT_LOGIN => JsonSerialization.tojson(
            UserDataService.fetchSenderList(None))
        case _ => JsonSerialization.tojson(UserDataService.fetchAllAdmin())
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getDetail(id:String):JsValue = {
    JsString("")
  }

  override def getForm(id:String):JsValue = {
    JsString("")
  }
}
