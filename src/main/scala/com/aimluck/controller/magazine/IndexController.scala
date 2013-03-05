package com.aimluck.controller.magazine;

import com.google.appengine.api.datastore.KeyFactory
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.controller.AbstractActionController
import com.aimluck.model.ContactGroup
import com.aimluck.service.ContactGroupService
import org.slim3.controller.Navigation

class IndexController extends AbstractActionController {
  override def getTemplateName:String = {
    org.dotme.liquidtpl.Constants.ACTION_INDEX_TEMPLATE
  }

  @throws(classOf[Exception])
  override protected def run():Navigation = {
    val contactGroupKey = request.getParameter("contactGroupKey")
    try {
      KeyFactory.stringToKey(contactGroupKey)
      return super.run
    } catch {
      case _ => 
        val redirectUri:String = ContactGroupService.fetchFirst(None) match {
          case null => "/contact/nogroup"
          case first:ContactGroup => "/magazine/index?contactGroupKey=%s"
            .format(KeyFactory.keyToString(first.getKey))
        }
        return redirect(redirectUri);
    }
    null
  }
}
