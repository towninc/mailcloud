package com.aimluck.controller.contact;

import com.aimluck.model.Contact
import com.aimluck.service.ContactService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import dispatch.json.JsObject
import dispatch.json.JsString
import dispatch.json.JsValue
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import org.slim3.datastore.S3QueryResultList
import scala.collection.mutable.ListBuffer
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class JsonController extends AbstractJsonDataController {
  val KEY_CONTACT_GROUP_KEY = "contactGroupKey";

  override def getList:JsValue = {
    import com.aimluck.service.ContactService.ContactProtocol._
    if(UserDataService.isUserAdmin) {
      val contactGroupKey = request.getParameter(KEY_CONTACT_GROUP_KEY);
      val _contactGroup = try {
        ContactGroupService.fetchOne(contactGroupKey, None)
      } catch {
        case e => None
      }

      val list:List[Contact] = _contactGroup match {
        case Some(contactGroup) => {
            val cursorNext = request.getParameter(Constants.KEY_CURSOR_NEXT)

            val result:S3QueryResultList[Contact] = cursorNext match {
              case null => {
                  ContactService.fetchResultList(contactGroup, None)
                }
              case v:String => {
                  ContactService.fetchResultList(contactGroup, Some(v))
                }
            }

            if(result.hasNext) {
              val uri = "/contact/json?contactGroupKey=%s".format(contactGroupKey)
              putExtraInformation(Constants.KEY_CURSOR_NEXT, "%s&%s=%s".format(
                  uri,
                  Constants.KEY_CURSOR_NEXT, result.getEncodedCursor
                ))
            }
            
            val buf:ListBuffer[Contact] = ListBuffer[Contact]()
            val it = result.iterator
            while(it.hasNext) {
              buf.append(it.next)
            }
            buf.toList
          }
        case None => List[Contact]()
      }

      putExtraInformation("delete", LanguageUtil.get("delete"))
      putExtraInformation("deleteConfirm", LanguageUtil.get("deleteListConform"
                                                            , Some(Array(LanguageUtil.get("contact")))))

      JsonSerialization.tojson(list)
    }else {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getDetail(id:String):JsValue = {
    import com.aimluck.service.ContactService.ContactProtocol._
    if(UserDataService.isUserAdmin) {
      ContactService.fetchOne(id, None) match {
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
    import com.aimluck.service.ContactService.ContactProtocol._
    if(UserDataService.isUserAdmin) {
      if((id != null) && (id.size > 0)){
        ContactService.fetchOne(id, None) match {
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
        tojson(ContactService.createNew)
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }
}
