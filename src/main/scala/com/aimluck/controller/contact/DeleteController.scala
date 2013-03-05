package com.aimluck.controller.contact;

import com.aimluck.model.Contact
import com.aimluck.service.ContactService

import com.aimluck.service.UserDataService
import dispatch.json.JsObject
import dispatch.json.JsString
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.helper.BasicHelper
import org.slim3.controller.Controller
import org.slim3.controller.Navigation

import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class DeleteController extends Controller {

  @throws(classOf[Exception])
  override protected def run():Navigation = {
    if(UserDataService.isUserAdmin) {
      val contactIds:Array[String] = request.getParameterValues("id[]");
      if(contactIds != null){
        contactIds.foreach{ id =>
          ContactService.fetchOne(id, None) match{
            case Some(contact) => 
              ContactService.delete(contact)
              QueueFactory.getQueue("import").
              add(Builder.withUrl("/tasks/contact/count")
                  .param("contactGroupKey", KeyFactory.keyToString(contact.getContactGroupRef.getKey))
                  .method(Method.POST));
            case None =>
          }
        }
      }
    }
    val contactGroupKey:String = request.getParameter("contactGroupKey");
    BasicHelper.writeJsonCommentFiltered(response, JsObject(List(
          (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_SUCCESS)),
          (JsString(Constants.KEY_REDIRECT), tojson("/contact/index?contactGroupKey=%s".format(contactGroupKey)))
        )))
    return null
  }
}
