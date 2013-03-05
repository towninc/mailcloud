package com.aimluck.controller.reminder;

import com.aimluck.service.ReminderService
import com.aimluck.service.UserDataService
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation

class DeleteController extends Controller {

  @throws(classOf[Exception])
  override protected def run():Navigation = {
    if(UserDataService.isUserAdmin) {
      request.getParameter(Constants.KEY_ID) match {
        case key:String =>
          ReminderService.fetchOne(key, None) match {
            case Some(v) => {
                ReminderService.delete(v)
              }
            case None =>
          }
      }
    }
        
    redirect("/reminder/index")
  }
}
