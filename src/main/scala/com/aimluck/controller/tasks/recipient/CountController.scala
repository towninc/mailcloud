package com.aimluck.controller.tasks.recipient;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.MailDataMeta
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.RecipientGroup
import com.aimluck.service.MailDataService
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions
import TaskOptions.{Builder, Method}
import java.util.Date
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import scala.collection.JavaConversions._

import org.slim3.datastore.Datastore

class CountController extends Controller {
  
  @throws(classOf[Exception])
  override def run():Navigation = {
    val m:MailDataMeta = MailDataMeta.get
    val mR:RecipientMeta = RecipientMeta.get

    
    // refix sentCount
    Datastore.query(m)
    .filter(m.status.equal(MailDataService.Status.INITIALIZING.toString))
    .asList.foreach{
      mailData =>
      var countRegistered:Long = 0
      val recipientGroupList:List[RecipientGroup] = mailData.getRecipientGroupRef.getModelList.toList
      if(recipientGroupList.size > 0) {
        var registerNotFinished:Boolean = false;
        recipientGroupList.foreach { recipientGroup =>
          countRegistered += recipientGroup.getRecipientCount.longValue
        }
      }

      response.getWriter.println("initializing mail data %s: %d/%d ".format(
          mailData.getKey,
          countRegistered,
          mailData.getRecipientCountAll
        ))

      if(countRegistered == mailData.getRecipientCountAll) {
        mailData.getRecipientGroupRef.getModelList.foreach{ rG =>
          val queueName = mailData.getMailType match {
            case AppConstants.MAILTYPE_MAGAZINE => "magazine"
            case AppConstants.MAILTYPE_STEPMAIL => "stepmail"
            case AppConstants.MAILTYPE_REMINDER => "reminder"
            case _ => "default"
          }

          QueueFactory.getQueue(queueName).
          add(Builder.withUrl("/tasks/mail/send")
              .param(Constants.KEY_ID, KeyFactory.keyToString(rG.getKey))
              .param(AppConstants.KEY_MAILTYPE, mailData.getMailType)
              .method(Method.POST));
          mailData.setStatus(MailDataService.Status.SENDING.toString)
          mailData.setSentAt(new Date)
        }
      }

      MailDataService.saveWithUserData(mailData, mailData.getUserDataRef.getModel)
    }

    return null;
  }
}
