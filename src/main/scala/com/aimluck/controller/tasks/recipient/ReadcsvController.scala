package com.aimluck.controller.tasks.recipient;

import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import au.com.bytecode.opencsv.CSVReader
import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.RecipientMeta
import com.aimluck.model.Recipient
import scala.collection.mutable.ListBuffer
import sjson.json.JsonSerialization._
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;

import com.aimluck.service.RecipientCsvService
import com.aimluck.service.RecipientGroupService
import com.aimluck.service.RecipientService
import com.aimluck.model.RecipientGroup
import com.aimluck.service.MailDataService
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.{ QueueFactory, TaskOptions }
import TaskOptions.{Builder, Method}

class ReadcsvController extends Controller {
  val logger = Logger.getLogger(classOf[ReadcsvController].getName)

  @throws(classOf[Exception])
  override def run():Navigation = {
    val recipientCsvKey = request.getParameter(AppConstants.KEY_RECIPIENT_CSV_KEY);
    val offset:Int = try{
      request.getParameter(AppConstants.KEY_CSV_OFFSET).toInt
    } catch {
      case _ => 0
    }
    
    println("add %s at %d".format(recipientCsvKey, offset))
    RecipientCsvService.fetchOne(recipientCsvKey) match {
      case Some(recipientCsv) =>
        val userData = recipientCsv.getUserDataRef.getModel
        val mailData = recipientCsv.getMailDataRef.getModel
        val mailDataKey:String = KeyFactory.keyToString(mailData.getKey)
        
        val reader = new java.io.InputStreamReader(
          new java.io.ByteArrayInputStream(
            RecipientCsvService.getBytes(recipientCsv)), recipientCsv.getCharset);

        try {
          val m:RecipientMeta = RecipientMeta.get();
          
          val recipientGroup:RecipientGroup = RecipientGroupService.createNew
          recipientGroup.setKey(Datastore.allocateId(classOf[RecipientGroup]))
          RecipientGroupService.saveWithMailData(recipientGroup, mailData)

          val recipientBuffer:ListBuffer[(String, Recipient)] = ListBuffer()
          val keys = Datastore
          .allocateIds(m, AppConstants.RECIPIENTS_PER_GROUP).iterator

          val csvReader:CSVReader = new CSVReader(reader);
          var nextLine:Array[String] = csvReader.readNext();
          var lineCount:Int = 0;
          var readCount:Int = 0;
          var isNotEnd:Boolean = false;
          while (nextLine != null) {
            if((nextLine.size > 0) && (nextLine.apply(0) != null)){
              //skip until offset
              if(lineCount >= offset){
                // read until count reaches group capacity
                if(readCount < AppConstants.RECIPIENTS_PER_GROUP){
                  val recipient:Recipient = RecipientService.createNew
                  val namespace:String = "%s:%s".format(mailDataKey, lineCount.toString);
                  recipient.setKey(keys.next)
                  recipient.setEmail(nextLine.apply(0).trim)
                  recipient.setReplacers(nextLine.toList)
                  recipient.getRecipientGroupRef.setModel(recipientGroup)
                  if(RecipientService.existsMark(recipient, namespace)){
                    logger.warning("Duplicate data %s (%s) skipped".format(recipient.getEmail, namespace))
                  } else {
                    recipientBuffer.append((namespace, recipient))
                  }
                  readCount += 1
                }
              }
              nextLine = csvReader.readNext()
              lineCount += 1

              if((lineCount - offset) > AppConstants.RECIPIENTS_PER_GROUP){
                isNotEnd = true
                //No need to read extra lines except first task
                if(offset > 0) {
                  nextLine = null;
                }
              }
            }
          }

          var isDataLimit = false
          //count lines only first task
          if(offset == 0) {
            mailData.setRecipientCountAll(lineCount)
            MailDataService.saveWithUserData(mailData, userData)
            val countAll = mailData.getRecipientCountAll.longValue + RecipientGroupService.countRecipientsToday
            if(countAll > AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY) {
              val error:String = "Exceeded the maximum number of %s e-mail per day: %s %/%s"
              .format(AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY, recipientCsvKey, countAll, AppConstants.DATA_LIMIT_RECIPIENT_PER_MAIL_DAY)
              logger.warning(error)
              mailData.setStatus(MailDataService.Status.ERROR.toString)
              mailData.setMessage(error)
              MailDataService.saveWithUserData(mailData, userData)
              mailData.getRecipientGroupRef.getModelList.foreach { rG =>
                RecipientGroupService.delete(rG)
              }
              isDataLimit = true;
            }
          }

          if(isDataLimit == false){
            val recipientList = recipientBuffer.toList
            var duplicateCount = 0
            recipientList.foreach { e =>
              try {
                RecipientService.saveMark(e._2, e._1)
                RecipientService.save(e._2)
              } catch {
                case e:DuplicateDataException => duplicateCount += 1
              }
            }

            recipientGroup.setRecipientCount(recipientList.size - duplicateCount)
            Datastore.put(recipientGroup)

            if(isNotEnd){
              QueueFactory.getQueue("import").
              add(Builder.withUrl("/tasks/recipient/readcsv")
                  .param(AppConstants.KEY_RECIPIENT_CSV_KEY, recipientCsvKey)
                  .param(AppConstants.KEY_CSV_OFFSET, (readCount + offset).toString).method(Method.POST));
            }
            QueueFactory.getQueue("default").
            add(Builder.withUrl("/tasks/recipient/count")
                .param(Constants.KEY_ID, KeyFactory.keyToString(mailData.getKey)).method(Method.POST));
          } 
        } catch {
          case e:Exception => {
              val error:String = "%s\n\t%s".format(e.getMessage, e.getStackTraceString).substring(0, 500);
              mailData.setStatus(MailDataService.Status.ERROR.toString)
              mailData.setMessage(error)
              MailDataService.saveWithUserData(mailData, userData)
              throw e
            }
        } finally {
          reader.close
        }
      case None =>
    }
    return null;
  }
}
