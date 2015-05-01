package com.aimluck.controller.contact;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.ContactGroup
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import dispatch.classic.json.JsString
import dispatch.classic.json.JsValue
import java.util.Date
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class GroupjsonController extends AbstractJsonDataController {

  override def getList:JsValue = {
    import com.aimluck.service.ContactGroupService.ContactGroupListProtocol._
    if (UserDataService.isUserAdmin) {
      val list:List[ContactGroup] = ContactGroupService.fetchAll(None)
      JsonSerialization.tojson(list)
    } else  {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getDetail(id:String):JsValue = {
    import com.aimluck.service.ContactGroupService.ContactGroupProtocol._
    if(UserDataService.isUserAdmin) {
      ContactGroupService.fetchOne(id, None) match {
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
    import com.aimluck.service.ContactGroupService.ContactGroupProtocol._
    if(UserDataService.isUserAdmin) {
      putExtraInformation("contactCreatedAt", AppConstants.dateTimeFormat.format(new Date))
      if((id != null) && (id.size > 0)){
        ContactGroupService.fetchOne(id, None) match {
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
        tojson(ContactGroupService.createNew)
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }
}
