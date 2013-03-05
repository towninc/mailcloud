package com.aimluck.controller.cron.mail;

import java.util.Date
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

import com.aimluck.lib.util.AppConstants
import com.aimluck.model.ContactGroup
import com.aimluck.model.MailData
import com.aimluck.service.StepMailService
import com.aimluck.service.MailMagazineService
import com.aimluck.service.MailDataService
import com.aimluck.service.ReminderService
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}
import org.slim3.datastore.Datastore

class AddController extends Controller {
  
  @throws(classOf[Exception])
  override def run():Navigation = {
    val date:Date = new Date
    val nowTime:Long = date.getTime
    val timeZoneId:String =  AppConstants.timeZone.getID
    StepMailService.fetchToSendByDate(date).foreach { stepMail =>
      val stepMailId = KeyFactory.keyToString(stepMail.getKey)
      val contactGoup:ContactGroup = stepMail.getContactGroupRef.getModel
      val mailData:MailData = MailDataService.createNew
      mailData.setSender(stepMail.getSender)
      mailData.setSubject(stepMail.getSubject)
      mailData.setContent(stepMail.getContent)
      mailData.setReplaced(stepMail.isReplaced)
      mailData.setMailType(AppConstants.MAILTYPE_STEPMAIL)
      MailDataService.saveWithUserData(mailData, stepMail.getUserDataRef.getModel)

      QueueFactory.getQueue("import")
      .add(Builder.withUrl("/tasks/recipient/readstepmail")
           .param(Constants.KEY_ID, stepMailId)
           .param("mailDataId", KeyFactory.keyToString(mailData.getKey))
           .param("nowTime", nowTime.toString)
      )
      
      stepMail.setLastSentAt(date)
      Datastore.put(stepMail)
      println("add stepMail queue %s".format(stepMailId))
    }

    MailMagazineService.fetchToSendByDate(date).foreach { mailMagazine =>
      val mailMagazineId = KeyFactory.keyToString(mailMagazine.getKey)
      val contactGoup:ContactGroup = mailMagazine.getContactGroupRef.getModel
      val mailData:MailData = MailDataService.createNew
      mailData.setSender(mailMagazine.getSender)
      mailData.setSubject(mailMagazine.getSubject)
      mailData.setContent(mailMagazine.getContent)
      mailData.setReplaced(mailMagazine.isReplaced)
      mailData.setMailType(AppConstants.MAILTYPE_MAGAZINE)
      MailDataService.saveWithUserData(mailData, mailMagazine.getUserDataRef.getModel)

      QueueFactory.getQueue("import")
      .add(Builder.withUrl("/tasks/recipient/readmagazine")
           .param(Constants.KEY_ID, mailMagazineId)
           .param("mailDataId", KeyFactory.keyToString(mailData.getKey)))

      mailMagazine.setActive(false)
      mailMagazine.setLastSentAt(date)
      Datastore.put(mailMagazine)
      println("add mailMagazine queue %s".format(mailMagazineId))
    }

    ReminderService.fetchToSendByDate(date).foreach { reminder =>
      val reminderId = KeyFactory.keyToString(reminder.getKey)
      val mailData:MailData = MailDataService.createNew
      mailData.setSender(reminder.getSender)
      mailData.setSubject(reminder.getSubject)
      mailData.setContent(reminder.getContent)
      mailData.setMailType(AppConstants.MAILTYPE_REMINDER)
      MailDataService.saveWithUserData(mailData, reminder.getUserDataRef.getModel)

      QueueFactory.getQueue("import")
      .add(Builder.withUrl("/tasks/recipient/readreminder")
           .param(Constants.KEY_ID, reminderId)
           .param("mailDataId", KeyFactory.keyToString(mailData.getKey)))

      reminder.setLastSentAt(date)
      Datastore.put(reminder)
      println("add reminder queue %s".format(reminderId))
    }
    return null;

  }
}
