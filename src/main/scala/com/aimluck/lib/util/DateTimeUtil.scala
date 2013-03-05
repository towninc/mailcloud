/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.lib.util

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.regex.Pattern

object DateTimeUtil {
  
  /**
   * 指定した2つの日付を比較する．
   *
   * @param date1
   * @param date2
   */
  def compareToDate(date1:Date, date2:Date, timeZone:TimeZone):Int = {
    val cal:Calendar = Calendar.getInstance( timeZone );

    cal.setTime(date1);
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val _date1:Date = cal.getTime

    cal.setTime(date2);
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val _date2:Date = cal.getTime

    return _date1.compareTo(_date2);
  }

  /**
   * 指定した2つの時間のみを比較する．
   *
   * @param date1
   * @param date2
   */
  def compareToTime(date1:Date, date2:Date, timeZone:TimeZone):Int = {
    val cal:Calendar = Calendar.getInstance( timeZone );

    cal.setTime(date1);
    cal.set(Calendar.YEAR, 1900)
    cal.set(Calendar.MONTH, 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val _date1:Date = cal.getTime

    cal.setTime(date2);
    cal.set(Calendar.YEAR, 1900)
    cal.set(Calendar.MONTH, 1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val _date2:Date = cal.getTime

    return _date1.compareTo(_date2);
  }

  /**
   * 
   */
  def newSendDateTime(date:Date):Date = {
    val cal:Calendar = Calendar.getInstance(AppConstants.timeZone)
    cal.setTime(date)
    cal.add(Calendar.HOUR, 1)
    cal.add(Calendar.DATE, 1)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTime
  }

  def getOneDayBefore(date:Date):Date = {
    val cal:Calendar = Calendar.getInstance(AppConstants.timeZone)
    cal.setTime(date)
    cal.add(Calendar.DATE, -1)
    cal.getTime
  }
}