package com.aimluck.controller.tasks.mail;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.MailData
import com.aimluck.service.RecipientGroupService
import com.aimluck.service.MailDataService
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.mail.MailService
import com.google.appengine.api.mail.MailServiceFactory
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import sjson.json.JsonSerialization._
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}


class SendController extends Controller {
  val logger = Logger.getLogger( classOf[SendController].getName )
  @throws(classOf[Exception])
  override def run():Navigation = {
    import sjson.json.DefaultProtocol._
    val id = request.getParameter(Constants.KEY_ID)
    logger.info("send start %s".format(id));
    val mailType:String= request.getParameter(AppConstants.KEY_MAILTYPE)
    try {
      RecipientGroupService.fetchOne(id) match {
        case Some(recipientGroup) => {
            val mailData:MailData = recipientGroup.getMailDataRef.getModel
            val ms = MailServiceFactory.getMailService // MailServiceを取得
            val m:RecipientMeta = RecipientMeta.get()
            Datastore.query(m)
            .filter(m.recipientGroupRef.equal(recipientGroup.getKey))
            .asList.toList.foreach { recipient =>
              val address:String = recipient.getEmail
              if(!MailDataService.existsMark(mailData, recipient)) {
                try {
                  val _mailMap = MailDataService.replacedMap(mailData, recipient)
                  val msg = new MailService.Message()
                  msg.setSubject(_mailMap.apply("subject"))
                  msg.setTo(address)
                  msg.setSender(mailData.getSender)
                  val body: String = _mailMap.apply("content") match {
                    case null => LanguageUtil.get("mailData.noContent")
                    case "" => LanguageUtil.get("mailData.noContent")
                    case string: String => string
                  }

                  msg.setTextBody(body)
                  ms.send(msg) // メール送信を実行
                  Datastore.put(MailDataService.createMark(mailData, recipient, mailType))
                  Datastore.delete(recipient.getKey)
                } catch {
                  case e:Exception =>
                    logger.severe(e.getMessage)
                    logger.severe(e.getStackTraceString)
                    val error:String = "%s\n\t%s".format(e.getMessage, e.getStackTraceString).substring(0, 500);
                    MailDataService.saveErrorMark(mailData, recipient, mailType, error)
                }
              }
            }

            QueueFactory.getQueue("default").
            add(Builder.withUrl("/tasks/mail/count")
                .param(Constants.KEY_ID, KeyFactory.keyToString(mailData.getKey)).method(Method.POST));

            val extraCount:Int = Datastore.query(m)
            .filter(m.recipientGroupRef.equal(recipientGroup.getKey))
            .count
            if(extraCount > 0){
              throw new RuntimeException
            }
          }
        case None =>
      }
    } catch {
      case e:Exception =>
        logger.severe(e.getMessage)
        logger.severe(e.getStackTraceString)
        throw new RuntimeException
    }
    return null;
  }
}
