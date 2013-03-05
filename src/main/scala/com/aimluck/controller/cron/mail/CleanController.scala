package com.aimluck.controller.cron.mail;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.MailDataMarkMeta
import com.aimluck.meta.MailDataMeta
import com.aimluck.meta.RecipientMarkMeta
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.UnsendMailData
import com.aimluck.service.MailDataService
import java.util.Calendar
import java.util.Date
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}
import com.aimluck.service.RecipientCsvService
import org.slim3.datastore.Datastore
import com.aimluck.service.ContactCsvService

class CleanController extends Controller {
  
  @throws(classOf[Exception])
  override def run():Navigation = {
    val m:MailDataMeta = MailDataMeta.get
    val mR:RecipientMeta = RecipientMeta.get
    // clean data
    val expiredDateCal:Calendar = Calendar.getInstance(AppConstants.SYSTEM_TIME_ZONE)
    expiredDateCal.setTime(new Date)
    expiredDateCal.add(Calendar.DATE, -1 * AppConstants.DATA_EXPIRE_DAYS)
    val expiredDay:Int = AppConstants.dayCountFormat.format(expiredDateCal.getTime).toInt

    MailDataService.fetchAll(None).foreach{
      mailData => val createdAtDay:String = mailData.getCreatedAtDay
      if((createdAtDay != null) && (createdAtDay.size > 0)){
        val createDate:Int = createdAtDay.toInt
        if (createDate < expiredDay) {
          if(mailData.getStatus != MailDataService.Status.FINISHED.toString) {
            val unsend = new UnsendMailData
            unsend.setSubject(mailData.getSubject)
            unsend.setContent(mailData.getContent)
            unsend.setSentAt(mailData.getSentAt)
            Datastore.put(unsend)
          }
          response.getWriter.println(mailData.getKey)
          Datastore.delete(mailData.getKey)
          QueueFactory.getQueue("mail").
          add(Builder.withUrl("/tasks/mail/cleanrecipients")
              .param(Constants.KEY_ID, KeyFactory.keyToString(mailData.getKey)));
        }
      }
    }
    
    ContactCsvService.fetchAll(None).foreach{
      contactCsv =>
      val createdAtDay:String = contactCsv.getCreatedAtDay
      if((createdAtDay != null) && (createdAtDay.size > 0)){
        val createDate:Int = createdAtDay.toInt
        if (createDate < expiredDay) {
          response.getWriter.println(contactCsv.getKey)
          ContactCsvService.delete(contactCsv)
        }
      }
    }

    RecipientCsvService.fetchAll(None).foreach{
      recipientCsv =>
      val createdAtDay:String = recipientCsv.getCreatedAtDay
      if((createdAtDay != null) && (createdAtDay.size > 0)){
        val createDate:Int = createdAtDay.toInt
        if (createDate < expiredDay) {
          response.getWriter.println(recipientCsv.getKey)
          RecipientCsvService.delete(recipientCsv)
        }
      }
    }

    
    val twoDaysAgoCal:Calendar = Calendar.getInstance(AppConstants.SYSTEM_TIME_ZONE)
    twoDaysAgoCal.setTime(new Date)
    twoDaysAgoCal.add(Calendar.DATE, -2)
    val twoDaysAgo:Int = AppConstants.dayCountFormat.format(expiredDateCal.getTime).toInt

    val rMM:RecipientMarkMeta = RecipientMarkMeta.get
    Datastore.query(rMM)
    .filter(rMM.createdAtDay.equal(twoDaysAgo.toString))
    .asKeyList.foreach{ k => Datastore.delete(k) }
    
    val mDM:MailDataMarkMeta = MailDataMarkMeta.get
    Datastore.query(mDM)
    .filter(mDM.sentAtDay.equal(twoDaysAgo.toString))
    .asKeyList.foreach{ k => Datastore.delete(k) }

    return null;
  }
}
