package com.aimluck.controller.tasks.recipient;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.ContactMeta
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.Contact
import com.aimluck.model.MailData
import com.aimluck.model.Recipient
import com.aimluck.model.RecipientGroup
import com.aimluck.service.RecipientService
import com.aimluck.service.RecipientGroupService
import com.aimluck.service.ReminderService
import com.aimluck.model.UserData
import com.aimluck.service.MailDataService
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import org.slim3.datastore.Datastore
import org.slim3.datastore.ModelQuery
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class ReadreminderController extends Controller {
  val logger = Logger.getLogger(classOf[ReadcsvController].getName)

  @throws(classOf[Exception])
  override def run():Navigation = {
    val reminderId = request.getParameter(Constants.KEY_ID);
    val cursor:String = try {
      request.getParameter(Constants.KEY_CURSOR_NEXT)
    } catch {
      case _ => null
    }
    val mailData:MailData = try {
      val key = KeyFactory.stringToKey(request.getParameter("mailDataId"))
      Datastore.get(classOf[MailData], key)
    } catch {
      case _ => null
    }

    if(mailData != null){
      val userData:UserData = mailData.getUserDataRef.getModel
      val mailDataKey:String = KeyFactory.keyToString(mailData.getKey)
      ReminderService.fetchOne(reminderId, None) match {
        case Some(reminder) => {
            try {
              val recipientBuffer:ListBuffer[(String, Recipient)] = ListBuffer()
              val recipientGroup:RecipientGroup = RecipientGroupService.createNew
              recipientGroup.setKey(Datastore.allocateId(classOf[RecipientGroup]))
              RecipientGroupService.saveWithMailData(recipientGroup, mailData)

              var isDataLimit = false
              //count lines only first task
              val size:Int = reminder.getRecipients.size
              mailData.setRecipientCountAll(size)
              MailDataService.saveWithUserData(mailData, userData)
              val countAll = mailData.getRecipientCountAll.longValue + RecipientGroupService.countRecipientsToday
              if(countAll > AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY) {
                val error:String = "Exceeded the maximum number of %s e-mail per day: %s %s/%s"
                .format(AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY, reminderId, countAll, AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY)

                logger.warning(error)
                mailData.setStatus(MailDataService.Status.ERROR.toString)
                MailDataService.saveWithUserData(mailData, userData)
                mailData.getRecipientGroupRef.getModelList.foreach { rG =>
                  RecipientGroupService.delete(rG)
                }
                isDataLimit = true;
              }

              if(mailData.getRecipientCountAll.intValue > 0){
                if(isDataLimit == false){
                  val listSize = mailData.getRecipientCountAll.longValue
                  val mR:RecipientMeta = RecipientMeta.get
                  val keys = Datastore.allocateIds(mR, listSize).iterator

                  reminder.getRecipients.foreach{
                    email => {
                      val recipient:Recipient = RecipientService.createNew
                      val namespace:String = "%s:%s".format(mailDataKey, KeyFactory.keyToString(reminder.getKey));
                      recipient.setKey(keys.next)
                      recipient.setEmail(email)
                      recipient.getRecipientGroupRef.setModel(recipientGroup)
                      if(RecipientService.existsMark(recipient, namespace)){
                        logger.warning("Duplicate data %s (%s) skipped".format(recipient.getEmail, namespace))
                      } else {
                        recipientBuffer.append((namespace, recipient))
                      }
                    }
                  }

                  val recipientList = recipientBuffer.toList
                  recipientGroup.setRecipientCount(recipientList.size)
                  Datastore.put(recipientGroup)
                  recipientList.foreach { e =>
                    RecipientService.saveMark(e._2, e._1)
                    RecipientService.save(e._2)
                  }

                  QueueFactory.getQueue("reminder").
                  add(Builder.withUrl("/tasks/recipient/count")
                      .param(Constants.KEY_ID, KeyFactory.keyToString(mailData.getKey)).method(Method.POST));
                }
              } else {
                Datastore.delete(mailData.getKey)
              }
            } catch {
              case e:Exception => {
                  val error:String = "%s\n\t%s".format(e.getMessage, e.getStackTraceString).substring(0, 500);
                  mailData.setStatus(MailDataService.Status.ERROR.toString)
                  mailData.setMessage(error)
                  MailDataService.saveWithUserData(mailData, userData)
                  throw e
                }
            }
          }
        case None =>
      }
    }
    return null;
  }
}
