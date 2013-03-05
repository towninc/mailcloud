package com.aimluck.controller.contact;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.ContactGroup
import com.aimluck.service.ContactCsvService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import java.text.ParseException
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.slim3.datastore.Datastore
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class GroupformController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)

  override def redirectUri:String = "/contact/group";

  override def getTemplateName:String = {
    "groupform"
  }

  def getRequestedEntity:ContactGroup = {
    if(UserDataService.isUserAdmin){
      val id = request.getParameter(Constants.KEY_ID);
      if((id == null) || ("" == id)) {
        ContactGroupService.createNew
      }else {
        ContactGroupService.fetchOne(id, None) match {
          case Some(v) => v
          case None => null
        }
      }
    } else {
      null
    }
  }

  override def validate:Boolean = {
    val contactGroup:ContactGroup = getRequestedEntity
    if(contactGroup != null) {
      //Name
      val name = request.getParameter("name")
      if(name.size <= 0 || name.size > AppConstants.VALIDATE_STRING_LENGTH){
        addError( "name" , LanguageUtil.get("error.stringLength",Some(Array(
                LanguageUtil.get("contactGroup.name"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.dataNotFound"))
    }
    val contactCsvKey = request.getParameter("contactCsvKey")
    if((contactCsvKey != null) && (contactCsvKey.size > 0)){
      try{
        ContactCsvService.fetchOne(contactCsvKey).get.getKey
      } catch {
        case e:Exception => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.noRecipient"));
      }
    }

    //contactCreatedAt
    val modifyContactCreatedAt = (request.getParameter("modifyContactCreatedAt") != null)
    val contactCreatedAt = request.getParameter("contactCreatedAt")
    if(modifyContactCreatedAt == true){
      try{
        AppConstants.dateTimeFormat.parse(contactCreatedAt)
      } catch {
        case e:ParseException => addError( "contactCreatedAt", LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("contactGroup.contactCreatedAt")))));
      }
    }
    !existsError
  }

  override def update:Boolean = {
    val contactGroup:ContactGroup = getRequestedEntity
    val id = request.getParameter(Constants.KEY_ID);
    val isNew = ((id == null) || ("" == id))
    if(!isNew && contactGroup.isBusy){
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("contactGroup.busyMessage"))
    } else {
      if(contactGroup != null){
        //Name
        contactGroup.setName(request.getParameter("name"))
        if(UserDataService.isUserAdmin) {
          val userData = UserDataService.getCurrentModel.get
          try {
            contactGroup.getUserDataRef.setModel(userData)
            val contactCsvKey = request.getParameter("contactCsvKey")
            if((contactCsvKey != null) && (contactCsvKey.size > 0)){
              ContactCsvService.fetchOne(contactCsvKey) match {
                case Some(contactCsv) => {
                    contactGroup.setBusy(true);
                    ContactCsvService.saveWithContactGroup(contactCsv, contactGroup)
                    // Csv read
                    val taskOptions:TaskOptions = Builder.withUrl("/tasks/contact/readcsv")
                    .param(AppConstants.KEY_CONTACT_CSV_KEY, contactCsvKey)
                    .param(AppConstants.KEY_CSV_OFFSET, "0").method(Method.POST)

                    if(request.getParameter("modifyContactCreatedAt") != null){
                      val contactCreatedAt = request.getParameter("contactCreatedAt")
                      val contactCreatedAtTime = AppConstants.dateTimeFormat.parse(contactCreatedAt).getTime
                      taskOptions.param("contactCreatedAt", contactCreatedAtTime.toString)
                    }

                    QueueFactory.getQueue("import").
                    add(taskOptions);
                  }
                case None => contactGroup.setBusy(false);
              }
            } else {
              contactGroup.setBusy(false);
            }
            ContactGroupService.saveWithUserData(contactGroup, userData)
          } catch {
            case e:DataLimitException => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.dataLimit", Some(Array(
                      LanguageUtil.get("contactGroup"), AppConstants.DATA_LIMIT_CONTACT_GROUP.toString))));
            case e:DuplicateDataException => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.duplicate", Some(Array(
                      LanguageUtil.get("contactGroup"), contactGroup.getName))));
          }
        } else {
          addError(Constants.KEY_GLOBAL_ERROR ,
                   LanguageUtil.get("error.sessionError"))
        }
      }
    }
    !existsError
  }
}