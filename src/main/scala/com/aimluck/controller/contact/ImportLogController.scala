package com.aimluck.controller.contact;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.ContactMeta
import com.aimluck.model.Contact
import com.aimluck.service.ContactGroupService
import com.aimluck.service.UserDataService
import java.io.Writer
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import org.slim3.datastore.Datastore
import org.slim3.datastore.ModelQuery
import org.slim3.datastore.S3QueryResultList
import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder

class ImportLogController extends Controller {
  val KEY_CONTACT_GROUP_KEY = "contactGroupKey";
  val KEY_CHARSET = "charset"

  @throws(classOf[Exception])
  override def run: Navigation = {
    import com.aimluck.service.ContactService.ContactProtocol._
    if (UserDataService.isUserAdmin) {
      val contactGroupKey = request.getParameter(KEY_CONTACT_GROUP_KEY);
      val charset = request.getParameter(KEY_CHARSET);
      val _contactGroup = try {
        ContactGroupService.fetchOne(contactGroupKey, None)
      } catch {
        case e => None
      }
      _contactGroup match {
        case Some(contactGroup) => {
          val contactCsv = ContactGroupService.getContactCsv(contactGroup)
          val log = ContactGroupService.getImportLogs(contactCsv).foreach { v =>
            response.getWriter().println(v.getContent())
          }
        }
        case None => {
          return redirect("/")
        }
      }
    }
    return null
  }

  private def writeResult(result: S3QueryResultList[Contact], writer: Writer): Unit = {
    val iterator = result.iterator
    while (iterator.hasNext) {
      val contact = iterator.next
      val lb: StringBuilder = new StringBuilder()
      contact.getReplacers.foreach { r =>
        lb.append(r)
        lb.append(",")
      }
      val line = lb.toString
      writer.write(line.substring(0, line.size - 1))
      writer.write("\r\n")
    }
  }
}
