package com.aimluck.controller.maildata;

import com.aimluck.lib.util.AppConstants
import com.aimluck.lib.util.TextUtil
import com.aimluck.service.RecipientCsvService
import com.aimluck.service.UserDataService
import org.slim3.controller.Controller
import org.slim3.controller.Navigation
import org.slim3.controller.upload.FileItem
import scala.collection.JavaConversions._
import com.google.appengine.api.datastore.KeyFactory
import dispatch.json.JsObject
import dispatch.json.JsString
import java.nio.charset.Charset
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.helper.BasicHelper
import sjson.json.JsonSerialization
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

class UploadController extends Controller {
  val KEY_URL = "url"
  val KEY_CHARSET = "charset"
  val KEY_CHARSET_MAP = "charsetMap"
  val FILE_FIELD = "formFile"
  val MODE_UPLOAD_URL = "uploadUrl"
  val CHARSET_MAP:List[(String, String)] = Charset.availableCharsets.filter{
    e => AppConstants.SUPPORTED_CSV_CHARSET.contains(e._1)
  }.map{
    e => (e._1, e._2.displayName())
  }.toList

  override protected def run():Navigation = {
    request.getParameter(Constants.KEY_MODE) match {
      case MODE_UPLOAD_URL =>{
          BasicHelper.writeJsonCommentFiltered(response, JsObject(List(
                (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_SUCCESS)),
                (JsString(KEY_URL), tojson("/maildata/upload?%s=%s".format(Constants.KEY_MODE, Constants.MODE_SUBMIT))),
                (JsString(KEY_CHARSET_MAP), BasicHelper.jsonFromStringPairs(CHARSET_MAP)),
                (JsString(KEY_CHARSET), tojson(AppConstants.DEFAULT_CSV_CHARSET))
              )))
        }
      case Constants.MODE_SUBMIT => {
          if(UserDataService.isUserAdmin) {
            val userData = UserDataService.getCurrentModel.get
            val formFile:FileItem = requestScope[FileItem](FILE_FIELD);
            if((formFile != null) && (formFile.getData.size > 0)) {
              println(requestScope[String](KEY_CHARSET) + " detected")
              try {
                val recipientCsv = RecipientCsvService.upload(formFile, requestScope[String](KEY_CHARSET))
                if(!TextUtil.validateEmail(recipientCsv.getSampleReplacers.apply(0))){
                  throw new RuntimeException(LanguageUtil.get("error.csvEmail", Some(Array("csv"))))
                }

                BasicHelper.writeJsonCommentFiltered(response, JsObject(List(
                      (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_SUCCESS)),
                      (JsString("name"), tojson(formFile.getFileName)),
                      (JsString("type"), tojson(formFile.getContentType)),
                      (JsString("sampleReplacers"), tojson(recipientCsv.getSampleReplacers.toList)),
                      (JsString(Constants.KEY_ID), tojson(KeyFactory.keyToString(recipientCsv.getKey)))
                    )))
              } catch {
                case e:Exception => e.printStackTrace
                  BasicHelper.writeJsonCommentFiltered(response, JsObject(List(
                        (JsString(Constants.KEY_RESULT), tojson(Constants.RESULT_FAILURE)),
                        (JsString("name"), tojson(e.getMessage)),
                        (JsString("type"), tojson(formFile.getContentType))
                      )))
              }
            } else {
              BasicHelper.writeJsonCommentFiltered(response, JsObject(List(
                    (JsString("name"), tojson(LanguageUtil.get("error.sessionError")))
                  )))
            }
          }
        }
      case _ =>
    }
    return null;
  }
}
