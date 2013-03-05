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


class CsvController extends Controller {
  val KEY_CONTACT_GROUP_KEY = "contactGroupKey";
  val KEY_CHARSET = "charset"

  @throws(classOf[Exception])
  override def run:Navigation = {
    import com.aimluck.service.ContactService.ContactProtocol._
    if(UserDataService.isUserAdmin) {
      val contactGroupKey = request.getParameter(KEY_CONTACT_GROUP_KEY);
      val charset = request.getParameter(KEY_CHARSET);
      val _contactGroup = try {
        ContactGroupService.fetchOne(contactGroupKey, None)
      } catch {
        case e => None
      }
      _contactGroup match {
        case Some(contactGroup) => {
            val m:ContactMeta = ContactMeta.get
            val query:ModelQuery[Contact] = Datastore.query(m)
            .filter(m.contactGroupRef.equal(contactGroup.getKey))
            .limit(AppConstants.MAX_COUNT);
            response.setContentType("application/x-csv; charset=%s".format(charset))
            response.setCharacterEncoding(charset)
            val writer = response.getWriter
            var result:S3QueryResultList[Contact] = query.asQueryResultList;
            writeResult(result, writer)
            while(result.hasNext) {
              result = Datastore.query(m)
              .encodedFilters(result.getEncodedFilters)
              .encodedEndCursor(result.getEncodedCursor)
              .limit(AppConstants.MAX_COUNT)
              .asQueryResultList
              writeResult(result, writer)
            }
          }
        case None => {
            return redirect("/")
          }
      }
    }
    return null
  }

  private def writeResult(result:S3QueryResultList[Contact], writer:Writer):Unit = {
    val iterator = result.iterator
    while(iterator.hasNext){
      val contact = iterator.next
      val lb:StringBuilder = new StringBuilder()
      contact.getReplacers.foreach{ r =>
        lb.append(r)
        lb.append(",")
      }
      val line = lb.toString
      writer.write(line.substring(0, line.size -1))
      writer.write("\r\n")
    }
  }
}
