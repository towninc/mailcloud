package com.aimluck.controller.tasks.contact;

import java.util.logging.Logger
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import org.slim3.datastore.Datastore
import com.aimluck.lib.util.AppConstants
import com.aimluck.lib.util.AppConstants
import com.aimluck.lib.util.TextUtil
import com.aimluck.meta.ContactMeta
import com.aimluck.model.Contact
import com.aimluck.model.ContactGroup
import com.aimluck.model.ContactMark
import com.aimluck.service.ContactCsvService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.ContactGroupService
import com.aimluck.service.ContactService
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.TaskOptions.Builder
import com.google.appengine.api.taskqueue.TaskOptions.Method
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions
import au.com.bytecode.opencsv.CSVReader
import sjson.json.JsonSerialization._
import com.aimluck.model.ContactImportLog

class ReadcsvController extends Controller {
  val logger = Logger.getLogger(classOf[ReadcsvController].getName)

  @throws(classOf[Exception])
  override def run(): Navigation = {
    val importLogBuf: StringBuilder = new StringBuilder
    val errorsBuf: ListBuffer[String] = new ListBuffer[String]()
    val contactCsvKey = request.getParameter(AppConstants.KEY_CONTACT_CSV_KEY);
    val offset: Int = try {
      request.getParameter(AppConstants.KEY_CSV_OFFSET).toInt
    } catch {
      case _ => 0
    }

    val paramContactCreatedAt = request.getParameter("contactCreatedAt")
    val contactCreatedAt: Date = if (paramContactCreatedAt != null) {
      try {
        val tempDate: Date = new Date
        tempDate.setTime(java.lang.Long.parseLong(paramContactCreatedAt))
        tempDate
      } catch {
        case _ => null
      }
    } else {
      null
    }

    importLogBuf.append("import %s at %d".format(contactCsvKey, offset)).append(Constants.LINE_SEPARATOR)
    ContactCsvService.fetchOne(contactCsvKey) match {
      case Some(contactCsv) =>
        val reader = new java.io.InputStreamReader(
          new java.io.ByteArrayInputStream(
            ContactCsvService.getBytes(contactCsv)), contactCsv.getCharset);
        try {
          val userData = contactCsv.getUserDataRef.getModel
          val contactGroup: ContactGroup = contactCsv.getContactGroupRef.getModel
          val contactGroupKey: String = KeyFactory.keyToString(contactGroup.getKey)
          val m: ContactMeta = ContactMeta.get();

          val contactBuffer: ListBuffer[Contact] = ListBuffer()
          val keys = Datastore
            .allocateIds(m, AppConstants.CONTACTS_PER_TASK).iterator

          val importLog = new ContactImportLog
          importLog.getUserDataRef.setKey(contactGroup.getUserDataRef.getKey)
          importLog.getContactGroupRef.setModel(contactGroup)
          importLog.getContactCsvRef.setModel(contactCsv)
          importLog.setCreatedAt(new Date)
          importLog.setLineCount(0)

          val csvReader: CSVReader = new CSVReader(reader);
          var nextLine: Array[String] = csvReader.readNext();
          var lineCount: Int = 0;
          var readCount: Int = 0;
          var overwrittenCount: Int = 0;
          var isNotEnd: Boolean = false;
          var newCount = 0;
          while (nextLine != null) {
            if ((nextLine.size > 0) && (nextLine.apply(0) != null)) {
              //skip until offset
              if (lineCount >= offset) {
                // read until count reaches group capacity
                if (readCount < AppConstants.CONTACTS_PER_TASK) {
                  val email = nextLine.apply(0).trim;
                  if (TextUtil.validateEmail(email)) {
                    val contact: Contact = ContactService.fetchMark(email, contactGroup) match {
                      case null => {
                        newCount += 1;
                        val _contact = ContactService.createNew
                        _contact.setEmail(email)
                        _contact.setKey(keys.next)
                        _contact
                      }
                      case mark: ContactMark => {
                        overwrittenCount += 1
                        logger.info("Contact %s is overwritten.".format(email))
                        importLogBuf.append("Contact %s is overwritten.".format(email)).append(Constants.LINE_SEPARATOR)
                        mark.getContactRef.getModel
                      }
                    }
                    contact.setReplacers(nextLine.toList)
                    contact.getContactGroupRef.setModel(contactGroup)
                    if (contactCreatedAt != null) {
                      ContactService.setCreatedAt(contact, contactCreatedAt)
                    }
                    contactBuffer.append(contact)
                    readCount += 1
                  } else {
                    logger.info("Contact %s is invalid.".format(email))
                    importLogBuf.append("Contact %s is invalid.".format(email)).append(Constants.LINE_SEPARATOR)
                    errorsBuf.append("%s:%s".format(email, ContactCsvService.ErrorType.INVALID_EMAIL.toString()))
                  }
                }
              }
              nextLine = csvReader.readNext()
              lineCount += 1

              if ((lineCount - offset) > AppConstants.CONTACTS_PER_TASK) {
                isNotEnd = true
                //No need to read extra lines except first task
                if (offset > 0) {
                  nextLine = null;
                }
              }
            }
          }

          val countAll = newCount + contactGroup.getContactCount.longValue
          val isDataLimit = (countAll > AppConstants.DATA_LIMIT_CONTACT_PER_GROUP)
          val contactList = contactBuffer.toList

          if (isDataLimit == false) {
            if (isNotEnd) {
              val taskOptions: TaskOptions = Builder.withUrl("/tasks/contact/readcsv")
                .param(AppConstants.KEY_CONTACT_CSV_KEY, contactCsvKey)
                .param(AppConstants.KEY_CSV_OFFSET, (readCount + offset).toString).method(Method.POST)

              if (contactCreatedAt != null) {
                val contactCreatedAtTime: Long = contactCreatedAt.getTime
                taskOptions.param("contactCreatedAt", contactCreatedAtTime.toString)
              }

              QueueFactory.getQueue("import").
                add(taskOptions);
            }

            var duplicateCount = 0
            contactList.foreach { contact =>
              try {
                ContactService.saveWithGroup(contact, contactGroup)
              } catch {
                case e: DuplicateDataException => {
                  duplicateCount += 1
                  importLogBuf.append("Contact %s is duplicated in csv file.".format(contact.getEmail())).append(Constants.LINE_SEPARATOR)
                  errorsBuf.append("%s:%s".format(contact.getEmail(), ContactCsvService.ErrorType.DUPLICATED.toString()))
                }
              }
            }
            contactGroup.setContactCount(countAll - duplicateCount)
            ContactGroupService.saveWithUserData(contactGroup, userData)

            importLogBuf.append("isNotEnd flag is %s, isDataLimit flag is %s".format(isNotEnd, isDataLimit)).append(Constants.LINE_SEPARATOR)

            if (!isNotEnd) {
              importLog.setLineCount(offset + readCount)
              importLogBuf.append("lineCount is %d".format(offset + readCount)).append(Constants.LINE_SEPARATOR)
              ContactCsvService.saveWithContactGroup(contactCsv, contactGroup)
              QueueFactory.getQueue("import").
                add(Builder.withUrl("/tasks/contact/count")
                  .param("contactGroupKey", KeyFactory.keyToString(contactGroup.getKey))
                  .method(Method.POST));
            }
          } else {
            val msg: String = "Data limit exceed %s %s/%s"
              .format(contactGroup.getKey, countAll, AppConstants.DATA_LIMIT_CONTACT_PER_GROUP)
            logger.severe(msg)
            contactGroup.setBusy(false)
            ContactGroupService.saveWithUserData(contactGroup, userData)

            importLogBuf.append(msg).append(Constants.LINE_SEPARATOR)
            importLogBuf.append("==============================").append(Constants.LINE_SEPARATOR)
            contactList.foreach { e =>
              importLogBuf.append(e.getEmail).append(Constants.LINE_SEPARATOR)
            }
            importLogBuf.append("==============================").append(Constants.LINE_SEPARATOR)
          }

          importLog.setErrorLines(seqAsJavaList(errorsBuf.toList))
          importLog.setOverwrittenCount(overwrittenCount)
          importLog.setContent(importLogBuf.toString())
          Datastore.put(importLog)

          ContactCsvService.saveWithContactGroup(contactCsv, contactGroup)
        } catch {
          case e: Exception => throw e
        } finally {
          reader.close
        }
      case None =>
    }
    return null;
  }
}
 