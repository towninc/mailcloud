package com.aimluck.controller.cron.mail;

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.MailDataMarkMeta
import com.aimluck.meta.RecipientMarkMeta
import java.util.Date
import org.slim3.controller.Controller;
import org.slim3.controller.Navigation;
import scala.collection.JavaConversions._
import org.slim3.datastore.Datastore

class DeletemarksController extends Controller {
  
  @throws(classOf[Exception])
  override def run():Navigation = {
    val nowDate:Int = AppConstants.dayCountFormat.format(new Date).toInt
    val date:Int = request.getParameter("date").toInt
    if(nowDate - 1 > date){
      val rMM:RecipientMarkMeta = RecipientMarkMeta.get
      Datastore.query(rMM)
      .filter(rMM.createdAtDay.equal(date.toString))
      .limit(200)
      .asKeyList.foreach{ k => response.getWriter.println("remove %s".format(k))
                         Datastore.delete(k) }
    
      val mDM:MailDataMarkMeta = MailDataMarkMeta.get
      Datastore.query(mDM)
      .filter(mDM.sentAtDay.equal(date.toString))
      .limit(200)
      .asKeyList.foreach{ k => response.getWriter.println("remove %s".format(k))
                         Datastore.delete(k) }
    }
    return null;
  }
}
