package com.aimluck.controller.reminder;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.Reminder
import com.aimluck.service.ReminderService
import com.aimluck.service.UserDataService
import java.text.DateFormat
import java.text.ParseException
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractFormController
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DataLimitException
import scala.collection.JavaConversions._
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class FormController extends AbstractFormController {
  override val logger = Logger.getLogger(classOf[FormController].getName)
  
  val dateTimeFormat:DateFormat = ReminderService.dateTimeFormatter;
  val dateFormat:DateFormat = ReminderService.dateFormatter;

  override def redirectUri:String = "/reminder/index";

  override def getTemplateName:String = {
    "form"
  }

  def getRequestedEntity:Reminder = {
    if(UserDataService.isUserAdmin){
      val id = request.getParameter(Constants.KEY_ID);
      if((id == null) || ("" == id)) {
        ReminderService.createNew
      }else {
        ReminderService.fetchOne(id, None) match {
          case Some(v) => v
          case None => null
        }
      }
    } else {
      null
    }
  }

  override def validate:Boolean = {

    //Subject
    val subject = request.getParameter("subject")
    if(subject.size <= 0 || subject.size > AppConstants.VALIDATE_STRING_LENGTH){
      addError( "subject" , LanguageUtil.get("error.stringLength",Some(Array(
              LanguageUtil.get("reminder.subject"), "1", AppConstants.VALIDATE_STRING_LENGTH.toString))));
    }

    //Content
    val content = request.getParameter("content")
    if(content.size > AppConstants.VALIDATE_LONGTEXT_LENGTH){
      addError( "content" , LanguageUtil.get("error.stringLength.max",Some(Array(
              LanguageUtil.get("reminder.content"), AppConstants.VALIDATE_LONGTEXT_LENGTH.toString))));
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

    //StartDate
    val startDate = request.getParameter("startDate")
    try{
      dateTimeFormat.setLenient(true)
      dateTimeFormat.parse(startDate)
    } catch {
      case e:ParseException => addError( "startDate" , LanguageUtil.get("error.invaldValue",Some(Array(
                LanguageUtil.get("reminder.startDate")))));
    }

    //EndDate
    val isEnd = (request.getParameter("isEnd") != null)
    val endDate = request.getParameter("endDate")
    if(isEnd == true){
      try{
        dateFormat.setLenient(true)
        dateFormat.parse(endDate)
      } catch {
        case e:ParseException => addError( "endDate" , LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("reminder.endDate")))));
      }
    }

    //Repeat
    val repeatType = request.getParameter("repeatType")
    try{
      val repeatCycle = request.getParameter("repeatCycle").toInt
      if (repeatType == ReminderService.RepeatType.Daily.toString) {
        if((repeatCycle < 0) || (repeatCycle > AppConstants.REPEAT_CYCLE_MAX_DAILY)){
          addError( "repeatCycle" , LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("reminder.repeatCycle"), "1", AppConstants.REPEAT_CYCLE_MAX_DAILY.toString))));
        }
      } else if (repeatType == ReminderService.RepeatType.Weekly.toString) {
        if((repeatCycle < 0) || (repeatCycle > AppConstants.REPEAT_CYCLE_MAX_WEEKLY)){
          addError( "repeatCycle" , LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("reminder.repeatCycle"), "1", AppConstants.REPEAT_CYCLE_MAX_WEEKLY.toString))));
        } else {
          val repeatWeekDays = request.getParameterValues("repeatWeekDays[]");
          if(repeatWeekDays == null || repeatWeekDays.size <= 0) {
            addError( "repeatWeekDays" , LanguageUtil.get("error.selectRequired", Some(Array(
                    LanguageUtil.get("reminder.repeatWeekDays")))));
          }
        }
      } else if (repeatType == ReminderService.RepeatType.Monthly.toString) {
        if((repeatCycle < 0) || (repeatCycle > AppConstants.REPEAT_CYCLE_MAX_MONTHLY)){
          addError( "repeatCycle" , LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("reminder.repeatCycle"), "1", AppConstants.REPEAT_CYCLE_MAX_MONTHLY.toString))));
        }
      } else if (repeatType == ReminderService.RepeatType.Yearly.toString) {
        if((repeatCycle < 0) || (repeatCycle > AppConstants.REPEAT_CYCLE_MAX_YEARLY)){
          addError( "repeatCycle" , LanguageUtil.get("error.invaldValue",Some(Array(
                  LanguageUtil.get("reminder.repeatCycle"), "1", AppConstants.REPEAT_CYCLE_MAX_YEARLY.toString))));
        }
      } else if (repeatType == ReminderService.RepeatType.None.toString) {
        null
      } else{
        addError( repeatType , LanguageUtil.get("error.invaldValue", Some(Array(
                LanguageUtil.get("reminder.repeatType")))));
      }
    } catch {
      case e:NumberFormatException => {
          if(repeatType != ReminderService.RepeatType.None.toString) {
            addError( "repeatCycle" , LanguageUtil.get("error.invaldValue",Some(Array(
                    LanguageUtil.get("reminder.repeatCycle")))));
          }
        }
    }

    //Recipients
    val recipientsText:String = request.getParameter("recipients");
    val recipients:List[String] = if(recipientsText != null) {
      recipientsText.split(Constants.LINE_SEPARATOR).toList.filter{ e =>
        e.trim.size > 0
      }
    } else {
      null
    }
    
    if(recipients == null || recipients.size <= 0){
      addError( "recipients" , LanguageUtil.get("error.required", Some(Array(
              LanguageUtil.get("reminder.recipients")))));
    } else if(recipients.size > AppConstants.DATA_LIMIT_RECIPIENTS_PER_REMINDER){
      addError( "recipients" , LanguageUtil.get("error.dataLimit", Some(Array(
              LanguageUtil.get("reminder.recipients"),
              AppConstants.DATA_LIMIT_RECIPIENTS_PER_REMINDER.toString
            ))));
    }
    !existsError
  }

  override def update:Boolean = {
    val reminder:Reminder = getRequestedEntity

    if(reminder != null){
      
      //Subject
      reminder.setSubject(request.getParameter("subject"))
      //Content
      reminder.setContent(request.getParameter("content"))
      
      //StartDate
      reminder.setStartDate(dateTimeFormat.parse(request.getParameter("startDate")))

      //EndDate
      reminder.setEnd(request.getParameter("isEnd") != null)
      if(reminder.isEnd){
        reminder.setEndDate(dateFormat.parse(request.getParameter("endDate")))
      }

      //Repeat
      reminder.setRepeatType(request.getParameter("repeatType"));
      if (reminder.getRepeatType == ReminderService.RepeatType.None.toString) {
        reminder.setRepeatCycle(null.asInstanceOf[Int])
        reminder.setRepeatWeekDays(null.asInstanceOf[Int])
        reminder.setEnd(null.asInstanceOf[Boolean])
        reminder.setEndDate(null)
      } else {
        reminder.setRepeatCycle(request.getParameter("repeatCycle").toInt)
      }

      //Recipients
      val recipients:List[String] = request.getParameter("recipients")
      .split(Constants.LINE_SEPARATOR).toList.filter{ e =>
        e.trim.size > 0
      }
      reminder.setRecipients(seqAsJavaList(recipients))

      if (reminder.getRepeatType == ReminderService.RepeatType.Weekly.toString) {
        ReminderService.setRepeatWeekDaysBySeq(
          request.getParameterValues("repeatWeekDays[]").map{
            i => i.toInt
          }.toSeq, reminder)
      }

      if(UserDataService.isUserAdmin) {
        val userData = UserDataService.getCurrentModel.get
        val senderEmail:String = request.getParameter("senderEmail")
        UserDataService.fetchByEmail(senderEmail) match {
          case Some(senderUserData) => {
              try {
                //Sender
                reminder.setSender("%s <%s>".format(request.getParameter("senderName"), senderEmail))
                ReminderService.saveWithUserData(reminder, userData)

                //update sender
                senderUserData.setName(request.getParameter("senderName"))
                UserDataService.save(senderUserData)
              } catch {
                case e:DataLimitException => addError( Constants.KEY_GLOBAL_ERROR , LanguageUtil.get("error.dataLimit", Some(Array(
                          LanguageUtil.get("reminder"), AppConstants.DATA_LIMIT_REMINDER.toString))));
              }
            }
          case None => addError(Constants.KEY_GLOBAL_ERROR ,
                                LanguageUtil.get("error.noDataError"))
        }
      } else {
        addError(Constants.KEY_GLOBAL_ERROR ,
                 LanguageUtil.get("error.sessionError"))
      }
    }
    !existsError
  }
}