package com.aimluck.controller.cron.contact;

import scala.collection.JavaConversions._
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import com.aimluck.meta.ContactGroupMeta
import org.slim3.datastore.Datastore
import com.aimluck.lib.util.AppConstants
import java.util.Date
import java.util.Calendar
import com.google.appengine.api.taskqueue.QueueFactory
import com.google.appengine.api.taskqueue.TaskOptions.Builder
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.taskqueue.TaskOptions.Method

class CountController extends Controller {

  @throws(classOf[Exception])
  override def run(): Navigation = {
    val m: ContactGroupMeta = ContactGroupMeta.get
    collectionAsScalaIterable(Datastore.query(m).filter(m.busy.equal(true)).limit(AppConstants.MAX_COUNT).asList()).filter {
      group =>
        val upDatedAt = group.getUpdatedAt()
        val upDatedAtCalMin = Calendar.getInstance;
        upDatedAtCalMin.setTime(upDatedAt)
        upDatedAtCalMin.add(Calendar.MINUTE, 10)
        
        val upDatedAtCalMax = Calendar.getInstance;
        upDatedAtCalMax.setTime(upDatedAt)
        upDatedAtCalMax.add(Calendar.MINUTE, 180)

        val now = new Date()
        val nowCal = Calendar.getInstance;
        nowCal.setTime(now)
        ((nowCal.getTime.after(upDatedAtCalMin.getTime()))
          && (nowCal.getTime.before(upDatedAtCalMax.getTime())))
    }.foreach{
      contactGroup => QueueFactory.getQueue("import").
              add(Builder.withUrl("/tasks/contact/count")
                  .param("contactGroupKey", KeyFactory.keyToString(contactGroup.getKey))
                  .method(Method.POST));
    }
    null
  }
}
