/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.DateTimeUtil
import com.aimluck.meta.ReminderMeta
import com.aimluck.model.Reminder
import com.aimluck.lib.util.AppConstants
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.logging.Logger
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.helper.BasicHelper
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization

object ReminderService {
  val logger = Logger.getLogger(ReminderService.getClass.getName)
  val dateTimeFormat:DateFormat = ReminderService.dateTimeFormatter;
  val dateFormat:DateFormat = ReminderService.dateFormatter;

  object RepeatType extends Enumeration {
    val None = Value("N")
    val Daily = Value("D")
    val Weekly = Value("W")
    val Monthly = Value("M")
    val Yearly = Value("Y")
  }

  object ReminderProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object ReminderFormat extends Format[Reminder] {
      override def reads(json: JsValue): Reminder = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(reminder: Reminder): JsValue = {
        val (senderName:String, senderEmail:String) =
          UserDataService.getSenderPair(reminder.getSender)

        JsObject(List(
            (tojson(Constants.KEY_ID).asInstanceOf[JsString], tojson(if(reminder.getKey != null) KeyFactory.keyToString(reminder.getKey) else null)),
            (JsString("subject"),  tojson(reminder.getSubject)),
            (JsString("content"),  tojson(reminder.getContent)),
            (JsString("startDate"), tojson(if(reminder.getStartDate != null) dateTimeFormat.format(reminder.getStartDate) else null)),
            (JsString("endDate"), tojson(if(reminder.getEndDate != null) dateTimeFormat.format(reminder.getEndDate) else null)),
            (JsString("repeatType"), tojson(reminder.getRepeatType)),
            (JsString("repeatCycle"),  tojson(reminder.getRepeatCycle)),
            (JsString("repeatWeekDays"), tojson(repeatWeekDaysList(reminder))),
            (JsString("recipients"), tojson(reminder.getRecipients.toList)),
            (JsString("isEnd"), tojson(reminder.isEnd)),
            (JsString("repeatTypeMap"), BasicHelper.jsonFromStringPairs(ReminderService.repeatTypeMap)),
            (JsString("repeatCycleDailyMap"), BasicHelper.jsonFromStringPairs(ReminderService.repeatCycleDailyMap)),
            (JsString("repeatCycleWeeklyMap"), BasicHelper.jsonFromStringPairs(ReminderService.repeatCycleWeeklyMap)),
            (JsString("repeatCycleMonthlyMap"), BasicHelper.jsonFromStringPairs(ReminderService.repeatCycleMonthlyMap)),
            (JsString("repeatCycleYearlyMap"), BasicHelper.jsonFromStringPairs(ReminderService.repeatCycleYearlyMap)),
            (JsString("repeatWeekDaysMapAll"), BasicHelper.jsonFromIntStringPairs(ReminderService.repeatWeekDaysMapAll)),
            (JsString("senderName"), tojson(senderName)),
            (JsString("senderEmail"), tojson(senderEmail)),
            (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("reminder"), reminder.getSubject)))))
          ))
      }
    }
  }

  object ReminderListProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object ReminderFormat extends Format[Reminder] {
      override def reads(json: JsValue): Reminder = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(reminder: Reminder): JsValue =
        JsObject(List(
            (tojson(Constants.KEY_ID).asInstanceOf[JsString], tojson(if(reminder.getKey != null) KeyFactory.keyToString(reminder.getKey) else null)),
            (JsString("subject"),  tojson(reminder.getSubject)),
            (JsString("startDate"), tojson(if(reminder.getStartDate != null) dateTimeFormat.format(reminder.getStartDate) else null)),
            (tojson(Constants.KEY_DELETE_CONFORM).asInstanceOf[JsString], tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("reminder"), reminder.getSubject)))))
          ))
    }
  }

  def fetchOne( id:String, _userData:Option[UserData] ):Option[Reminder] = {
    val m:ReminderMeta = ReminderMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      _userData match {
        case Some(userData) =>{
            Datastore.query(m).filter(m.key.equal(key))
            .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
              case v:Reminder => Some(v)
              case null => None
            }
          }
        case None => {
            Datastore.query(m).filter(m.key.equal(key)).asSingle match {
              case v:Reminder => Some(v)
              case null => None
            }
          }
      }

    } catch {
      case e:Exception => {
          logger.severe(e.getMessage)
          logger.severe(e.getStackTraceString)
          None
        }
    }
  }

  def fetchAll(_userData:Option[UserData]):List[Reminder] = {
    val m:ReminderMeta = ReminderMeta.get
    _userData match {
      case Some(userData) => Datastore.query(m).filter(m.userDataRef.equal(userData.getKey)).asList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def createNew():Reminder = {
    val result:Reminder = new Reminder
    result.setSubject("")
    result.setContent("")
    result.setStartDate(DateTimeUtil.newSendDateTime(new Date))
    result.setEndDate(DateTimeUtil.newSendDateTime(new Date))
    result.setEnd(false)
    result.setRepeatType(RepeatType.None.toString)
    result.setRecipients(List[String]())
    result
  }

  def saveWithUserData(model:Reminder, userData:UserData):Key = {
    val key:Key = model.getKey

    //Data Limit
    if(key == null){
      if(fetchAll(Some(userData)).size >= AppConstants.DATA_LIMIT_REMINDER){
        throw new DataLimitException
      }
    }

    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
      model.setLastSentAt(
        DateTimeUtil.getOneDayBefore(now)
      )
    }

    model.setUpdatedAt(now)
    model.getUserDataRef.setModel(userData)
    model.setTimeZone(AppConstants.timeZone.getID)
    Datastore.put(userData, model).apply(1)
  }


  def delete(reminder:Reminder){
    Datastore.delete(reminder.getKey)
  }

  val repeatTypeMap:List[(String, String)] = List[(String, String)](
    RepeatType.None.toString -> LanguageUtil.get("repeatType.none"),
    RepeatType.Daily.toString -> LanguageUtil.get("repeatType.daily"),
    RepeatType.Weekly.toString -> LanguageUtil.get("repeatType.weekly"),
    RepeatType.Monthly.toString -> LanguageUtil.get("repeatType.monthly"),
    RepeatType.Yearly.toString -> LanguageUtil.get("repeatType.yearly")
  )

  val repeatCycleDailyMax:Int = {
    var max:Int = 1;
    try {
      max = AppConstants.REPEAT_CYCLE_MAX_DAILY
    }catch{
      case e:Exception => max = 1;
    }
    max
  }

  val repeatCycleDailyMap:List[(String, String)] = {
    val buf:ListBuffer[(String, String)] = ListBuffer[(String, String)]()
    for(i <- 1 to repeatCycleDailyMax){
      if( i > 1 ){
        buf.append(Integer.toString(i) -> (Integer.toString(i) + LanguageUtil.get("repeatType.daily.suffix")))
      }else{
        buf.append(Integer.toString(i) -> LanguageUtil.get("repeatType.daily.every"))
      }
    }
    buf.toList
  }
  val repeatCycleWeeklyMax:Int = {
    var max:Int = 1;
    try {
      max = AppConstants.REPEAT_CYCLE_MAX_WEEKLY
    }catch{
      case e:Exception => max = 1;
    }
    max
  }

  val repeatCycleWeeklyMap:List[(String, String)] = {
    var buf:ListBuffer[(String, String)] = ListBuffer[(String, String)]()
    for(i <- 1 to repeatCycleWeeklyMax){
      if( i > 1 ){
        buf.append(Integer.toString(i) -> (Integer.toString(i) + LanguageUtil.get("repeatType.weekly.suffix")))
      }else{
        buf.append(Integer.toString(i) -> LanguageUtil.get("repeatType.weekly.every"))
      }
    }
    buf.toList
  }

  val repeatCycleMonthlyMax:Int = {
    var max:Int = 1;
    try {
      max = AppConstants.REPEAT_CYCLE_MAX_MONTHLY
    }catch{
      case e:Exception => max = 1;
    }
    max
  }

  val repeatCycleMonthlyMap:List[(String, String)] = {
    var buf:ListBuffer[(String, String)] = ListBuffer[(String, String)]()
    for(i <- 1 to repeatCycleMonthlyMax){
      if( i > 1 ){
        buf.append(Integer.toString(i) -> (Integer.toString(i) + LanguageUtil.get("repeatType.monthly.suffix")))
      }else{
        buf.append(Integer.toString(i) -> LanguageUtil.get("repeatType.monthly.every"))
      }
    }
    buf.toList
  }

  val repeatCycleYearlyMax:Int = {
    var max:Int = 1;
    try {
      max = AppConstants.REPEAT_CYCLE_MAX_YEARLY
    }catch{
      case e:Exception => max = 1;
    }
    max
  }

  val repeatCycleYearlyMap:List[(String, String)] = {
    var buf:ListBuffer[(String, String)] = ListBuffer[(String, String)]()
    for(i <- 1 to repeatCycleYearlyMax){
      if( i > 1 ){
        buf.append(Integer.toString(i) -> (Integer.toString(i) + LanguageUtil.get("repeatType.yearly.suffix")))
      }else{
        buf.append(Integer.toString(i) -> LanguageUtil.get("repeatType.yearly.every"))
      }
    }
    buf.toList
  }


  val repeatWeekDaysMapAll:List[(Int, String)] = List[(Int, String)](
    scala.math.pow(2, Calendar.SUNDAY).asInstanceOf[Int] -> LanguageUtil.get("sunday"),
    scala.math.pow(2, Calendar.MONDAY).asInstanceOf[Int] -> LanguageUtil.get("monday"),
    scala.math.pow(2, Calendar.TUESDAY).asInstanceOf[Int] -> LanguageUtil.get("tuesday"),
    scala.math.pow(2, Calendar.WEDNESDAY).asInstanceOf[Int] -> LanguageUtil.get("wednesday"),
    scala.math.pow(2, Calendar.THURSDAY).asInstanceOf[Int] -> LanguageUtil.get("thursday"),
    scala.math.pow(2, Calendar.FRIDAY).asInstanceOf[Int] -> LanguageUtil.get("friday"),
    scala.math.pow(2, Calendar.SATURDAY).asInstanceOf[Int] -> LanguageUtil.get("saturday")
  )

  val repeatWeekDaysListAll:List[Int] = repeatWeekDaysMapAll.map { _._1 }

  def repeatWeekDaysList(reminder:Reminder):List[Int] = (1 to 7)
  .map(scala.math.pow(2, _).asInstanceOf[Int])
  .filter(i => (i & reminder.getRepeatWeekDays) != 0).toList;

  def setRepeatWeekDaysBySeq(s:Seq[Int], reminder:Reminder) = {
    var result:Int = 0;
    s.foreach{
      i => result += i
    }
    reminder.setRepeatWeekDays(result)
  }

  def fetchByTerm(startDate:Date, endDate:Date, _userData:Option[UserData]):List[Reminder] = {
    var result:List[Reminder] = Nil
    val timeZone:TimeZone = AppConstants.timeZone
    val endDateCal:Calendar = Calendar.getInstance( timeZone )
    endDateCal.setTime(endDate)
    endDateCal.add(Calendar.DATE, 1)
    val endDatePlus:Date = endDateCal.getTime

    val m:ReminderMeta = ReminderMeta.get

    result = fetchAll(_userData).filter{ m =>
      m.getStartDate.compareTo(endDatePlus) < 0
    }.filter(m =>
      if(m.getRepeatType == RepeatType.None.toString){
        ((m.getStartDate.compareTo(startDate)) >= 0)
      }else if(m.isEnd == true){
        ((m.getEndDate.compareTo(startDate)) >= 0)
      }else{
        true
      }
    )
    .sortWith((x,y) => x.getStartDate.before(y.getStartDate) )
    result
  }

  def dailyContainer(startDate:Date, endDate:Date, _userData:Option[UserData]):List[(Date, List[Reminder])] = {
    var buf:ListBuffer[(Date, List[Reminder])] = ListBuffer[(Date, List[Reminder])]()
    val timeZone:TimeZone = AppConstants.timeZone
    val cal:Calendar = Calendar.getInstance( timeZone )
    val reminders = fetchByTerm(startDate, endDate, _userData)
    cal.setTime(startDate)
    while(cal.getTime.before(endDate)){
      val date:Date = cal.getTime
      buf.append((date -> reminders.filter{
            m => isInDay(m, date)
          }))
      cal.add(Calendar.DATE, 1)
    }
    buf.toList
  }

  /**
   * 指定した繰り返しパターンにマッチするかどうかを返します。
   *
   * @param date
   * @param ptn
   * @param startDate
   * @param limitDate
   * @return
   */
  def isInDay(reminder:Reminder, date:Date):Boolean = {
    var is_repeat:Boolean = true;
    var result:Boolean = false;
    val timeZone = try{
      TimeZone.getTimeZone(reminder.getTimeZone)
    } catch {
      case _ => AppConstants.timeZone
    }

    val startDateTime:Date = reminder.getStartDate;
    val cal:Calendar = Calendar.getInstance( timeZone )
    cal.setTime(date);
    val startCal:Calendar = Calendar.getInstance( timeZone )
    startCal.setTime(startDateTime);
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)
    startCal.set(Calendar.MILLISECOND, 0)
    val startDate:Date = startCal.getTime

    val cycle:Int = reminder.getRepeatCycle
    // 毎日
    if (reminder.getRepeatType == RepeatType.Daily.toString) {
      val diffDays:Long = ((date.getTime - startDate.getTime) / (1000 * 60 * 60 * 24))
      if((cycle == 0) || ((diffDays % cycle) == 0)){
        result = true;
      }
      // 毎週
    } else if (reminder.getRepeatType == RepeatType.Weekly.toString) {
      var weekMatch:Boolean = false;
      val dayOfWeek:Int = cal.get(Calendar.DAY_OF_WEEK)
      val dayOfWeekBit:Int = scala.math.pow(2, dayOfWeek).toInt
      repeatWeekDaysList(reminder).foreach{
        i => if( dayOfWeekBit == i){
          weekMatch = true
        }
      }

      if(weekMatch){
        //開始日以後最初の曜日を取得
        val tempStartCal:Calendar = Calendar.getInstance( timeZone );
        tempStartCal.setTime(startDate);
        while(tempStartCal.get(Calendar.DAY_OF_WEEK) != dayOfWeek){
          tempStartCal.add(Calendar.DATE, 1)
        }
        val tempStartDate:Date = tempStartCal.getTime
        val diffWeeks:Long = ((date.getTime - tempStartDate.getTime) / (1000 * 60 * 60 * 24 * 7))
        if((diffWeeks % reminder.getRepeatCycle) == 0){
          result = true;
        }
      }
      // 毎月
    } else if (reminder.getRepeatType == RepeatType.Monthly.toString) {
      if(startCal.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)){
        val startMonth:Int = ((startCal.get(Calendar.MONTH) + 1) * startCal.get(Calendar.YEAR))
        val dateMonth:Int = ((cal.get(Calendar.MONTH) + 1) * cal.get(Calendar.YEAR))
        if(((dateMonth - startMonth) % reminder.getRepeatCycle) == 0){
          result = true
        }
      }
    } else if (reminder.getRepeatType == RepeatType.Yearly.toString) {
      if((startCal.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH))
         && (startCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH))){
        val startYear:Int = startCal.get(Calendar.YEAR)
        val dateYear:Int = cal.get(Calendar.YEAR)
        if(((dateYear - startYear) % reminder.getRepeatCycle) == 0){
          result = true
        }
      }
    } else {
      if(DateTimeUtil.compareToDate(startDate, date, timeZone) == 0){
        result = true
      }
    }

    if (result) {
      if ((reminder.getRepeatType != RepeatType.None.toString)) {
        if(((reminder.isEnd == true) 
            && (DateTimeUtil.compareToDate(reminder.getEndDate, date, timeZone) < 0)) ||
           (DateTimeUtil.compareToDate(startDate, date, timeZone) > 0) ){
          result = false;
        }
      }
    }
    return result;
  }

  def fetchAllInSystemByDate(date:Date):List[Reminder] = {
    var result:List[Reminder] = Nil
    val m:ReminderMeta = ReminderMeta.get
    result = Datastore.query(m).asList.toList
    result = result.filter{ reminder =>
      val timeZone = try{
        TimeZone.getTimeZone(reminder.getTimeZone)
      } catch {
        case _ => AppConstants.timeZone
      }
      val startDateCal:Calendar = Calendar.getInstance( timeZone )
      startDateCal.setTime(date)
      startDateCal.set(Calendar.HOUR_OF_DAY, 0)
      startDateCal.set(Calendar.MINUTE, 0)
      startDateCal.set(Calendar.SECOND, 0)
      startDateCal.set(Calendar.MILLISECOND, 0)
      val startDate:Date = startDateCal.getTime
      val endDateCal:Calendar = Calendar.getInstance( timeZone )
      endDateCal.setTime(startDate)
      endDateCal.add(Calendar.DATE, 1)
      val endDatePlus:Date = endDateCal.getTime
      ((reminder.getStartDate.compareTo(endDatePlus) < 0) && (isInDay(reminder, startDate)))
    }
    .sortWith((x, y) => x.getStartDate.before(y.getStartDate) )
    result
  }

  def fetchToSendByDate(date:Date):List[Reminder] = {
    val list:List[Reminder] = fetchAllInSystemByDate(date).filter{
      reminder =>
      //通知予定時刻の算出
      val startDate:Date = reminder.getStartDate
      val modelTimeZone = try{
        TimeZone.getTimeZone(reminder.getTimeZone)
      } catch {
        case _ => AppConstants.timeZone
      }
      val dateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(date)
      val lastSentDateStr = AppConstants.dayCountFormatWithTimeZone(modelTimeZone).format(reminder.getLastSentAt)

      val dateTimeCalendar = Calendar.getInstance( modelTimeZone )
      dateTimeCalendar.setTime(date)
      val startTimeCalendar = Calendar.getInstance( modelTimeZone )
        
      startTimeCalendar.setTime(startDate)
      startTimeCalendar.set(Calendar.YEAR, dateTimeCalendar.get(Calendar.YEAR))
      startTimeCalendar.set(Calendar.MONTH, dateTimeCalendar.get(Calendar.MONTH))
      startTimeCalendar.set(Calendar.DATE, dateTimeCalendar.get(Calendar.DATE))

      //分補正
      startTimeCalendar.add(Calendar.SECOND, -60)

      //通知予定時刻前の場合
      (
        (dateStr.toInt > lastSentDateStr.toInt)
        && (startTimeCalendar.getTimeInMillis <= dateTimeCalendar.getTimeInMillis)
      )
    }
    list
  }

  def dateTimeFormatter(timezone:Option[TimeZone]):DateFormat = {
    val dateTimeFormat:DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm")
    timezone match {
      case Some(v) => dateTimeFormat.setTimeZone(v)
      case None=> dateTimeFormat.setTimeZone( AppConstants.timeZone )
    }
    dateTimeFormat
  }

  def dateTimeFormatter():DateFormat = {
    dateTimeFormatter(None)
  }

  def dateFormatter(timezone:Option[TimeZone]):DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("yyyy/MM/dd")
    timezone match {
      case Some(v) => dateFormat.setTimeZone(v)
      case None=> dateFormat.setTimeZone( AppConstants.timeZone )
    }
    dateFormat
  }

  def dateFormatter():DateFormat = {
    dateFormatter(None)
  }
}
