package com.aimluck.controller.tasks.contact;

import com.aimluck.service.ContactGroupService
import java.util.logging.Logger
import org.slim3.controller.Controller
import org.slim3.controller.Navigation


class CleanController extends Controller {
  val logger = Logger.getLogger(classOf[CleanController].getName)

  @throws(classOf[Exception])
  override def run():Navigation = {
    val contactGroupKey = request.getParameter("contactGroupKey");
    ContactGroupService.cleanByKey(contactGroupKey)
    return null;
  }
}
