package com.aimluck.controller.reminder;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.Reminder
import com.aimluck.service.ReminderService

import com.aimluck.service.UserDataService
import dispatch.classic.json.JsValue
import java.util.Calendar
import java.util.Date
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonDataController
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._

class JsonController extends AbstractJsonDataController {
  val KEY_TERM = "term";
  
  override def getList:JsValue = {
    import com.aimluck.service.ReminderService.ReminderListProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin) {
      val list:List[Reminder] = request.getParameter(KEY_TERM) match {
        case null =>
          ReminderService.fetchAll(None)
        case _ =>
          getTermList
      }
      JsonSerialization.tojson(list)
    } else {
      addError(Constants.KEY_GLOBAL_ERROR, LanguageUtil.get("error.sessionError"))
      null
    }
  }


  def getTermList:List[Reminder] = {
    val todayCal:Calendar = Calendar.getInstance( AppConstants.timeZone )
    todayCal.setTime(new Date)
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)
    val today:Date = todayCal.getTime

    val startDate:Date = request.getParameter("start_date") match {
      case null => today
      case v => {
          try {
            ReminderService.dateFormatter.parse(v)
          } catch {
            case e:Exception => today
          }
        }
    }

    val endDateCal:Calendar = Calendar.getInstance( AppConstants.timeZone )
    endDateCal.setTime(startDate)
    endDateCal.add(Calendar.DATE, 7)
    val endDate:Date = endDateCal.getTime

    val cal:Calendar = Calendar.getInstance( AppConstants.timeZone )
    cal.setTime(startDate)
    if(UserDataService.isUserAdmin){
      ReminderService.fetchByTerm(startDate, startDate, None).filter{
        ReminderService.isInDay(_, startDate)
      }
    }else {
      null
    }
  }


  override def getDetail(id:String):JsValue = {
    import com.aimluck.service.ReminderService.ReminderProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin){
      ReminderService.fetchOne(id, None) match {
        case Some(v) => {
            tojson(v)
          }
        case None => {
            addError(Constants.KEY_GLOBAL_ERROR,
                     LanguageUtil.get("error.dataNotFound"))
            null
          }
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }

  override def getForm(id:String):JsValue = {
    import com.aimluck.service.ReminderService.ReminderProtocol._
    val startDate:Date =  new Date
    if(UserDataService.isUserAdmin){
      if((id != null) && (id.size > 0)){
        ReminderService.fetchOne(id, None) match {
          case Some(v) => {
              if(!v.isEnd){
                v.setEndDate(v.getStartDate)
              }
              tojson(v)
            }
          case None => {
              addError(Constants.KEY_GLOBAL_ERROR,
                       LanguageUtil.get("error.dataNotFound"))
              null
            }
        }
      } else {
        tojson(ReminderService.createNew)
      }
    } else {
      addError(Constants.KEY_GLOBAL_ERROR,
               LanguageUtil.get("error.sessionError"))
      null
    }
  }
}
