package com.aimluck.controller.tasks.contact;

import com.aimluck.meta.ContactMeta
import com.aimluck.service.ContactGroupService
import java.util.logging.Logger
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import org.slim3.datastore.Datastore
import com.aimluck.model.ContactCsv

class CountController extends Controller {
  val logger = Logger.getLogger(classOf[CountController].getName)

  @throws(classOf[Exception])
  override def run(): Navigation = {
    val contactGroupKey = request.getParameter("contactGroupKey");
    val m: ContactMeta = ContactMeta.get
    ContactGroupService.fetchOne(contactGroupKey, None) match {
      case Some(contactGroup) =>
        val count = Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).count
        contactGroup.setContactCount(count)

        if (contactGroup.isBusy) {
          val contactCsv: ContactCsv = ContactGroupService.getContactCsv(contactGroup)
          val importLogs = ContactGroupService.getImportLogs(contactCsv)
          val lineCount = ContactGroupService.getLineCount(importLogs)
          if (lineCount > 0) {
            val importCount: Int = ContactGroupService.getErrorLines(importLogs) match {
              case null => lineCount - ContactGroupService.getOverwrittenCount(importLogs)
              case errorLines => lineCount - ContactGroupService.getErrorLines(importLogs).size - ContactGroupService.getOverwrittenCount(importLogs)
            }
            if ((count == importCount)) {
              contactGroup.setBusy(false)
            }
          }
        }
        Datastore.put(contactGroup)
      case None =>
    }
    return null;
  }
}
