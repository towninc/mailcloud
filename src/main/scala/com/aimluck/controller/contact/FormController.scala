package com.aimluck.controller.contact;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.Contact
import com.aimluck.service.ContactService
import com.aimluck.service.UserDataService
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DuplicateDataException
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class FormController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)
  
  override def redirectUri:String = "/contact/index";

  override def getTemplateName:String = {
    "form"
  }

  def getRequestedEntity:Contact = {
    if(UserDataService.isUserAdmin) {
      val id = request.getParameter(Constants.KEY_ID);
      if((id == null) || ("" == id)) {
        // Unavailable to insert new model by form
        null
      } else {
        ContactService.fetchOne(id, None) match {
          case Some(v) => v
          case None => null
        }
      }
    } else {
      null
    }
  }

  override def validate:Boolean = {
    val contact:Contact = getRequestedEntity
    if(contact != null) {
      //Name
      val name = request.getParameter("name")
      if(name.size <= 0 || name.size > AppConstants.VALIDATE_STRING_LENGTH){
        addError( "name" , LanguageUtil.get("error.stringLength",Some(Array(
                LanguageUtil.get("contact.name"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
      }

      //email
      if(!contact.isSelf) {
        val email = request.getParameter("email")
        if(email.size > AppConstants.VALIDATE_STRING_LENGTH){
          addError( "email" , LanguageUtil.get("error.stringLength.max",Some(Array(
                  LanguageUtil.get("contact.email"), AppConstants.VALIDATE_STRING_LENGTH.toString))));
        }
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.dataNotFound"))
    }
    !existsError
  }

  override def update:Boolean = {
    val contact:Contact = getRequestedEntity

    if(contact != null){
      
      //Name
      contact.setName(request.getParameter("name"))

      //Email
      if(!contact.isSelf) {
        contact.setEmail(request.getParameter("email"))
      }

      if(UserDataService.isUserAdmin){
        try {
          ContactService.saveWithGroup(contact, contact.getContactGroupRef.getModel)
        } catch {
          case e:DuplicateDataException => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.duplicate", Some(Array(
                    LanguageUtil.get("contact"), contact.getEmail))));
        }
      } else {
        addError(Constants.KEY_GLOBAL_ERROR ,
                 LanguageUtil.get("error.sessionError"))
      }
    }
    !existsError
  }
}