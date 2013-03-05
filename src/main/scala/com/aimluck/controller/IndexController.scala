/* To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.controller

import com.aimluck.lib.util.AppConstants
import com.google.appengine.api.datastore.DatastoreService
import com.google.appengine.api.datastore.DatastoreServiceFactory
import com.google.appengine.api.datastore.Query
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import scala.collection.JavaConversions._

class IndexController extends Controller {
  @throws(classOf[Exception])
  override protected def run():Navigation = {
    val datastoreService = DatastoreServiceFactory.getDatastoreService
    datastoreService.prepare(new Query("cG")).asIterator.foreach{ e =>
      e.removeProperty("nUI")
      datastoreService.put(e)
    }

    datastoreService.prepare(new Query("mM")).asIterator.foreach{ e =>
      if(e.getProperty("iA") == null)
      {
        e.setProperty("iA", false);
      }
      datastoreService.put(e)
    }

    redirect("/maildata/index")
  }
}
