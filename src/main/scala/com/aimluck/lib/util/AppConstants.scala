/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.lib.util

import java.io.File
import java.io.FileNotFoundException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone

object AppConstants {
  val CONFIG_FOLDER = "contents"
  val CONFIG_FILE = "app"
  val SYSTEM_TIME_ZONE:TimeZone = TimeZone.getTimeZone( "GMT-8:00" )

  val DEFAULT_CSV_CHARSET = "Shift_JIS"
  val SUPPORTED_CSV_CHARSET:List[String] = List(
    "Shift_JIS",
    "UTF-8",
    "EUC-JP"
  )

  // settings for validate
  val VALIDATE_STRING_LENGTH = 100
  val VALIDATE_LONGTEXT_LENGTH = 10000

  // settings for mailData
  val DATA_LIMIT_FILE_SIZE = 5242880
  val DATA_LIMIT_FILE_SIZE_STRING = "5MB"
  val DATA_LIMIT_RECIPIENT_PER_MAIL_DAY = 100000
  val DATA_LIMIT_CONTACT_PER_GROUP = 100000
  val DATA_LIMIT_CONTACT_GROUP = 100
  val DATA_LIMIT_REMINDER = 1000
  val DATA_LIMIT_RECIPIENTS_PER_REMINDER = 10
  val DATA_EXPIRE_DAYS = 30
  val UNREGISTERED_DATA_EXPIRE_DAYS = 1
  val MAX_COUNT = 1000
  val RECIPIENTS_PER_GROUP = 500
  val CONTACTS_PER_TASK = 500
  val MAX_STEP_DAYS = 90
  val DEFAULT_STEPMAIL_SENDTIME = "10:00"

  val KEY_CSV_OFFSET = "offset"
  val KEY_RECIPIENT_CSV_KEY = "recipientCsvKey"
  val KEY_CONTACT_CSV_KEY = "contactCsvKey"
  val KEY_MAILTYPE = "mailType"

  val MAILTYPE_CSV = "C";
  val MAILTYPE_STEPMAIL = "S";
  val MAILTYPE_MAGAZINE = "M";
  val MAILTYPE_REMINDER = "R";

  val ICON_SRC_LIST = "/img/icon/16/notepad.gif"
  val ICON_SRC_EDIT = "/img/icon/16/pencil.gif"
  val ICON_SRC_DELETE = "/img/icon/16/button-delete.gif"
  val IMG_SRC_INDICATOR = "/img/common/ajax-loader.gif"

  val RESULTS_PER_PAGE:Int = 100

  // settings for reminder
  val MINUTE_STEP = 5
  val REPEAT_CYCLE_MAX_DAILY = 30
  val REPEAT_CYCLE_MAX_WEEKLY = 30
  val REPEAT_CYCLE_MAX_MONTHLY = 30
  val REPEAT_CYCLE_MAX_YEARLY = 30

  private def configPath:String = {
    try{
      CONFIG_FOLDER + File.separator + CONFIG_FILE
    }catch{
      case e:FileNotFoundException => "."
    }
  }

  private val DEFAULT_TIME_ZONE:TimeZone = TimeZone.getTimeZone( "Asia/Tokyo" )
  def timeZone:TimeZone = DEFAULT_TIME_ZONE;

  def dayCountFormat:DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("yyyyMMdd")
    dateFormat.setTimeZone(AppConstants.SYSTEM_TIME_ZONE)
    dateFormat
  }

  def dayCountFormatWithTimeZone(timeZone:TimeZone):DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("yyyyMMdd")
    dateFormat.setTimeZone(timeZone)
    dateFormat
  }

  def timeFormat:DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("HH:mm")
    dateFormat.setTimeZone(AppConstants.timeZone)
    dateFormat
  }

  def dateTimeFormat:DateFormat = {
    val dateFormat:DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm")
    dateFormat.setTimeZone(AppConstants.timeZone)
    dateFormat
  }

}