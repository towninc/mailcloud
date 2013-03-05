package com.aimluck.controller.stepmail;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.ContactGroup
import com.aimluck.model.StepMail
import com.aimluck.service.StepMailService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import java.text.DateFormat
import java.text.ParseException
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

class FormController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)
  
  val dateTimeFormat:DateFormat = StepMailService.dateTimeFormatter;
  val dateFormat:DateFormat = StepMailService.dateFormatter;

  override def redirectUri:String = "/stepmail/index?contactGroupKey=" + request.getParameter("formContactGroupKey");

  override def getTemplateName:String = {
    "form"
  }

  def getRequestedEntity:StepMail = {
    val contactGroupKey = request.getParameter("contactGroupKey")
    ContactGroupService.fetchOne(contactGroupKey, None) match {
      case Some(contactGroup) =>
        val formContactGroupKey = request.getParameter("formContactGroupKey")
        ContactGroupService.fetchOne(formContactGroupKey, None) match {
          case Some(formContactGroup) => {
              if(UserDataService.isUserAdmin) {
                val id = request.getParameter(Constants.KEY_ID);
                if((id == null) || ("" == id)) {
                  StepMailService.createNew(formContactGroup)
                
                }else {
                  StepMailService.fetchOne(id, None) match {
                    case Some(v) => {
                        StepMailService.setContactGroup(v, formContactGroup)
                        v
                      }
                    case None => null
                  }
                }
              } else {
                null
              }
            }
          case None => null
        }
      case None => null
    }
  }

  override def validate:Boolean = {
    if(UserDataService.isUserAdmin) {
      val contactGroupKey = request.getParameter("contactGroupKey")
      val formContactGroupKey = request.getParameter("formContactGroupKey")
      try{
        val contactGroup:ContactGroup = ContactGroupService.fetchOne(contactGroupKey, None).get
        val formContactGroup:ContactGroup = ContactGroupService.fetchOne(formContactGroupKey, None).get
        //Subject
        val subject = request.getParameter("subject")
        if(subject.size <= 0 || subject.size > AppConstants.VALIDATE_STRING_LENGTH){
          addError( "subject" , LanguageUtil.get("error.stringLength",Some(Array(
                  LanguageUtil.get("stepMail.subject"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
        }

        //Content
        val content = request.getParameter("content")
        if(content.size > AppConstants.VALIDATE_LONGTEXT_LENGTH){
          addError( "content" , LanguageUtil.get("error.stringLength.max",Some(Array(
                  LanguageUtil.get("stepMail.content"), AppConstants.VALIDATE_LONGTEXT_LENGTH.toString))));
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

        //sendTime
        val sendTime = request.getParameter("sendTime")
        try{
          AppConstants.timeFormat.parse(sendTime)
        } catch {
          case e:ParseException => addError( "sendTime" , LanguageUtil.get("error.invaldValue",Some(Array(
                    LanguageUtil.get("stepMail.sendTime")))));
        }

        //intervalDays
        try{
          val intervalDays = request.getParameter("intervalDays").toInt
          val maxIntervalDays = StepMailService.count(contactGroup) + 1
          if((intervalDays < 0) || (intervalDays > AppConstants.MAX_STEP_DAYS)){
            addError( "intervalDays" , LanguageUtil.get("error.invaldValue",Some(Array(
                    LanguageUtil.get("stepMail.intervalDays"), "1", AppConstants.MAX_STEP_DAYS.toString))));
          }
        } catch {
          case e:NumberFormatException => {
              addError( "intervalDays" , LanguageUtil.get("error.invaldValue",
                                                          Some(Array(LanguageUtil.get("stepMail.intervalDays")))));
            }
        }

      
      } catch {
        case e:Exception => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.selectRequired",
                                                                                    Some(Array(LanguageUtil.get("contactGroup")))));
      }
      
    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.sessionError"))
    }

    !existsError
  }

  override def update:Boolean = {
    if(UserDataService.isUserAdmin) {
      val userData = UserDataService.getCurrentModel.get
      val senderEmail:String = request.getParameter("senderEmail")
      UserDataService.fetchByEmail(senderEmail) match {
        case Some(senderUserData) => {
            val stepMail:StepMail = getRequestedEntity
            if(stepMail != null){
              try {
                //Subject
                stepMail.setSubject(request.getParameter("subject"))
                //Content
                stepMail.setContent(request.getParameter("content"))

                //Sender
                stepMail.setSender("%s <%s>".format(request.getParameter("senderName"), senderEmail))
                
                //sendTime
                val sendTime = request.getParameter("sendTime")
                stepMail.setSendTime(AppConstants.timeFormat.parse(sendTime))

                //intervalDays
                val intervalDays = request.getParameter("intervalDays").toInt
                stepMail.setIntervalDays(intervalDays)

                StepMailService.saveWithUserData(stepMail, userData)

                //update sender
                senderUserData.setName(request.getParameter("senderName"))
                UserDataService.save(senderUserData)
              } catch {
                case e:Exception => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.systemError"));
              }
            } else {
              addError(Constants.KEY_GLOBAL_ERROR ,
                       LanguageUtil.get("error.noDataError"))
            }
          }
        case None => addError(Constants.KEY_GLOBAL_ERROR ,
                              LanguageUtil.get("error.noDataError"))
      }

    } else {
      addError(Constants.KEY_GLOBAL_ERROR ,
               LanguageUtil.get("error.sessionError"))
    }
    !existsError
  }
}