package com.aimluck.controller.magazine;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.ContactGroup
import com.aimluck.model.MailMagazine
import com.aimluck.service.MailMagazineService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import java.text.DateFormat
import java.text.ParseException
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

class FormController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)
  
  val dateTimeFormat:DateFormat = MailMagazineService.dateTimeFormatter;
  val dateFormat:DateFormat = MailMagazineService.dateFormatter;

  override def redirectUri:String = "/magazine/index?contactGroupKey=" + request.getParameter("formContactGroupKey");

  override def getTemplateName:String = {
    "form"
  }

  def getRequestedEntity:MailMagazine = {
    val contactGroupKey = request.getParameter("contactGroupKey")
    ContactGroupService.fetchOne(contactGroupKey, None) match {
      case Some(contactGroup) =>
        val formContactGroupKey = request.getParameter("formContactGroupKey")
        ContactGroupService.fetchOne(formContactGroupKey, None) match {
          case Some(formContactGroup) => {
              if(UserDataService.isUserAdmin) {
                val id = request.getParameter(Constants.KEY_ID);
                if((id == null) || ("" == id)) {
                  MailMagazineService.createNew(formContactGroup)
                
                } else {
                  MailMagazineService.fetchOne(id, None) match {
                    case Some(v) => {
                        MailMagazineService.setContactGroup(v, formContactGroup)
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
                  LanguageUtil.get("mailMagazine.subject"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
        }

        //Content
        val content = request.getParameter("content")
        if(content.size > AppConstants.VALIDATE_LONGTEXT_LENGTH){
          addError( "content" , LanguageUtil.get("error.stringLength.max",Some(Array(
                  LanguageUtil.get("mailMagazine.content"), AppConstants.VALIDATE_LONGTEXT_LENGTH.toString))));
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

        //sendDateTime
        val sendDateTime = request.getParameter("sendDateTime")
        try{
          val sendDateTimeVal:Date = AppConstants.dateTimeFormat.parse(sendDateTime)
          if(sendDateTimeVal.before(new Date)){
            addError( "sendDateTime" , LanguageUtil.get("error.invaldValue",Some(Array(
                    LanguageUtil.get("mailMagazine.sendDateTime")))));
          }
        } catch {
          case e:ParseException => addError( "sendDateTime" , LanguageUtil.get("error.invaldValue",
                                                                               Some(Array(LanguageUtil.get("mailMagazine.sendDateTime")))));
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
            val mailMagazine:MailMagazine = getRequestedEntity
            if(mailMagazine != null){
              try {
                //Subject
                mailMagazine.setSubject(request.getParameter("subject"))
                //Content
                mailMagazine.setContent(request.getParameter("content"))

                //Sender
                mailMagazine.setSender("%s <%s>".format(request.getParameter("senderName"), senderEmail))

                //sendDateTime
                val sendDateTime = request.getParameter("sendDateTime")
                mailMagazine.setSendDateTime(AppConstants.dateTimeFormat.parse(sendDateTime))
                if(mailMagazine.getSendDateTime.after(new Date)){
                  mailMagazine.setActive(true)
                }

                MailMagazineService.saveWithUserData(mailMagazine, userData)

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