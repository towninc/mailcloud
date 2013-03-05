package com.aimluck.controller.tasks.mail;

import com.aimluck.meta.MailDataMarkMeta
import com.aimluck.meta.MailDataMeta
import com.aimluck.service.MailDataService
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import scala.collection.JavaConversions._

import org.slim3.datastore.Datastore

class CountController extends Controller {
  
  @throws(classOf[Exception])
  override def run():Navigation = {
    val m:MailDataMeta = MailDataMeta.get
    val mDM:MailDataMarkMeta = MailDataMarkMeta.get
    // refix sentCount
    Datastore.query(m)
    .filter(m.status.equal(MailDataService.Status.SENDING.toString))
    .asList.foreach { mailData =>
      var newCount = 0
      val countAll:Long = mailData.getRecipientCountAll.longValue
      mailData.getRecipientGroupRef.getModelList.foreach { recipientGroup =>
        newCount += Datastore.query(mDM).filter(mDM.recipientGroupRef.equal(recipientGroup.getKey)).count
      }

      mailData.setSentCount(newCount)
      if(newCount == countAll){
        mailData.setStatus(MailDataService.Status.FINISHED.toString)
      }
      MailDataService.saveWithUserData(mailData, mailData.getUserDataRef.getModel)
      response.getWriter.println("sending %s: %d/%d ".format(
          mailData.getKey,
          newCount,
          countAll
        ))
    }
    return null;
  }
}
