package com.aimluck.controller.userdata;

import com.aimluck.service.UserDataService
import java.util.logging.Logger
import org.dotme.liquidtpl.controller.AbstractFormController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class AssignformController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[AssignformController].getName)
  
  override def redirectUri:String = "/userdata/assignform";

  override def getTemplateName:String = {
    "assignform"
  }

  override def validate:Boolean = {
    !existsError
  }

  override def update:Boolean = {
    if(UserDataService.isUserAdmin){
      UserDataService.getCurrentModel match {
        case Some(userData) => val assign = request.getParameter("assign");
          if(assign == "true") {
            userData.setCommonSender(true);
          } else {
            userData.setCommonSender(false);
          }
          UserDataService.save(userData)
        case None =>
      }

    }
    !existsError
  }
}