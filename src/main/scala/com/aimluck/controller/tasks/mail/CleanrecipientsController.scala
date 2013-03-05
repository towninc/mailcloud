package com.aimluck.controller.tasks.mail;

import com.aimluck.service.MailDataService
import com.aimluck.service.UserDataService
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.slim3.controller.Controller
import org.slim3.controller.Navigation


class CleanrecipientsController extends Controller {
  val logger = Logger.getLogger(classOf[CleanrecipientsController].getName)

  @throws(classOf[Exception])
  override def run():Navigation = {
    val id = request.getParameter(Constants.KEY_ID);
    MailDataService.fetchOne(id, None) match {
      case Some(mailData) =>
        logger.info("clean %s".format(id))
        MailDataService.cleanRecipients(mailData)
      case None => logger.warning("not found %s".format(id))
    }
    return null;
  }
}
