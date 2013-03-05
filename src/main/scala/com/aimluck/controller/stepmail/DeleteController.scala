package com.aimluck.controller.stepmail;

import com.aimluck.service.StepMailService

import com.aimluck.service.UserDataService
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

import com.google.appengine.api.datastore.KeyFactory
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation



class DeleteController extends Controller {

  @throws(classOf[Exception])
  override protected def run():Navigation = {
    if(UserDataService.isUserAdmin) {
      request.getParameter(Constants.KEY_ID) match {
        case key:String =>
          StepMailService.fetchOne(key, None) match {
            case Some(v) => {
                val contactGroupKey = KeyFactory.keyToString(v.getContactGroupRef.getKey)
                StepMailService.delete(v)
                return redirect("/stepmail/index?contactGroupKey=%s".format(contactGroupKey))
              }
            case None =>
          }
      }
    }
    return null
  }
}
