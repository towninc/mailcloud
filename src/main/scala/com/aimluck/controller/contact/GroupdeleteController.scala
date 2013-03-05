package com.aimluck.controller.contact;

import com.aimluck.model.ContactGroup
import com.aimluck.service.ContactGroupService

import com.aimluck.service.UserDataService
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation

import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class GroupdeleteController extends Controller {

  @throws(classOf[Exception])
  override protected def run():Navigation = {
    if(UserDataService.isUserAdmin) {
      request.getParameter(Constants.KEY_ID) match {
        case key:String =>
          ContactGroupService.fetchOne(key, None) match {
            case Some(v) => {
                ContactGroupService.delete(v)
                QueueFactory.getDefaultQueue
                .add(Builder.withUrl("/tasks/contact/clean")
                     .param("contactGroupKey", key)
                     .method(Method.POST));
              }
            case None =>
          }
      }
    }
    redirect("/contact/group")
  }
}
