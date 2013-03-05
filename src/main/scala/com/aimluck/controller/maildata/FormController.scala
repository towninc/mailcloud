package com.aimluck.controller.maildata;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.MailData
import com.aimluck.service.MailDataService
import com.aimluck.service.RecipientCsvService
import com.aimluck.service.UserDataService
import java.text.DateFormat
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import org.slim3.datastore.Datastore
import sjson.json.DefaultProtocol._
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class FormController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)
  
  val dateTimeFormat:DateFormat = MailDataService.dateTimeFormatter;
  val dateFormat:DateFormat = MailDataService.dateFormatter;

  override def redirectUri:String = "/maildata/index";

  override def getTemplateName:String = {
    "form"
  }

  override def validate:Boolean = {
    if(UserDataService.isUserAdmin) {
      //Subject
      val subject = request.getParameter("subject")
      if(subject.size <= 0 || subject.size > AppConstants.VALIDATE_STRING_LENGTH){
        addError( "subject" , LanguageUtil.get("error.stringLength",Some(Array(
                LanguageUtil.get("mailData.subject"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
      }

      //Content
      val content = request.getParameter("content")
      if(content.size > AppConstants.VALIDATE_LONGTEXT_LENGTH){
        addError( "content" , LanguageUtil.get("error.stringLength.max",Some(Array(
                LanguageUtil.get("mailData.content"), AppConstants.VALIDATE_LONGTEXT_LENGTH.toString))));
      }

      //Sender
      val senderName = request.getParameter("senderName")
      if(senderName.size <= 0 || senderName.size > AppConstants.VALIDATE_STRING_LENGTH){
        addError( "sender" , LanguageUtil.get("error.stringLength",Some(Array(
                LanguageUtil.get("mailData.sender"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
      }

      //Sender
      val senderEmail = request.getParameter("senderEmail")
      if(senderEmail.size <= 0 || senderEmail.size > AppConstants.VALIDATE_STRING_LENGTH){
        addError( "sender" , LanguageUtil.get("error.stringLength",Some(Array(
                LanguageUtil.get("mailData.sender"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
      } else {
        val senderEmail:String = request.getParameter("senderEmail")
        UserDataService.fetchByEmail(senderEmail) match {
          case Some(userData) =>
          case None => addError(Constants.KEY_GLOBAL_ERROR ,
                                LanguageUtil.get("error.noSender"))
        }
      }

      val recipientCsvKey = request.getParameter("recipientCsvKey")
      try{
        RecipientCsvService.fetchOne(recipientCsvKey).get.getKey
      } catch {
        case e:Exception => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.noRecipient"));
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.sessionError"))
    }

    !existsError
  }

  override def update:Boolean = {
    // Unavailable to edit existing data
    val mailData:MailData = MailDataService.createNew
    if(mailData != null){
      if(UserDataService.isUserAdmin) {
        val userData = UserDataService.getCurrentModel.get
        val senderEmail:String = request.getParameter("senderEmail")
        UserDataService.fetchByEmail(senderEmail) match {
          case Some(senderUserData) => {
              try {
                //Subject
                mailData.setSubject(request.getParameter("subject"))
                //Content
                mailData.setContent(request.getParameter("content"))
                //Sender
                mailData.setSender("%s <%s>".format(request.getParameter("senderName"), senderEmail))
                senderUserData.setName(request.getParameter("senderName"))
                UserDataService.save(senderUserData)
                    
                val recipientCsvKey = request.getParameter("recipientCsvKey")
                val recipientCsv = RecipientCsvService.fetchOne(recipientCsvKey).get

                mailData.setMailType(AppConstants.MAILTYPE_CSV)
                MailDataService.saveWithUserData(mailData, userData)
                RecipientCsvService.saveWithMailData(recipientCsv, mailData)

                // Csv read
                QueueFactory.getQueue("import").
                add(Builder.withUrl("/tasks/recipient/readcsv")
                    .param(AppConstants.KEY_RECIPIENT_CSV_KEY, recipientCsvKey)
                    .param(AppConstants.KEY_CSV_OFFSET, "0").method(Method.POST));
              } catch {
                case e:Exception => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.systemError"));
              }
            }
          case None =>
            addError(Constants.KEY_GLOBAL_ERROR ,
                     LanguageUtil.get("error.noSender"))
        }
           
      } else {
        addError(Constants.KEY_GLOBAL_ERROR ,
                 LanguageUtil.get("error.sessionError"))
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.sessionError"))
    }
    !existsError
  }
}