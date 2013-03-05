package com.aimluck.controller.magazine;

import com.aimluck.service.MailMagazineService

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
          MailMagazineService.fetchOne(key, None) match {
            case Some(v) => {
                val contactGroupKey = KeyFactory.keyToString(v.getContactGroupRef.getKey)
                MailMagazineService.delete(v)
                return redirect("/magazine/index?contactGroupKey=%s".format(contactGroupKey))
              }
            case None =>
          }
      }
    }
    return null
  }
}
