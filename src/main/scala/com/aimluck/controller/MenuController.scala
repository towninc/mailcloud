/* To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.controller

import com.aimluck.lib.util.AppConstants
import dispatch.json.JsObject
import dispatch.json.JsString
import dispatch.json.JsValue
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.controller.AbstractJsonController
import scala.collection.JavaConversions._
import sjson.json.JsonSerialization._
import sjson.json.DefaultProtocol._

//FIXME Case Classで実装して一般化する
class MenuController extends AbstractJsonController {
  def getJson:JsValue = {
    val PATH:String = "path"
    val NAME:String = "name"
    val TEXT:String = "text"
    val CLASS:String = "css_class"
    val SELECT:String = "select"

    val MAIN_MENU:String = "mainMenu"
    val SUB_MENU:String = "subMenu"

    val pageBasePath = request.getParameter(Constants.KEY_BASE_PATH);
    val sitemap:List[Map[String, String]] = List(
      Map(PATH -> "/maildata/index", TEXT -> LanguageUtil.get("home")),
      Map(PATH -> "/contact/group", TEXT -> LanguageUtil.get("contactGroup")),
      Map(PATH -> "/magazine/index", TEXT -> LanguageUtil.get("mailMagazine")),
      Map(PATH -> "/stepmail/index", TEXT -> LanguageUtil.get("stepMail")),
      Map(PATH -> "/reminder/index", TEXT -> LanguageUtil.get("reminder")),
      Map(PATH -> "/userdata/assignform", TEXT -> LanguageUtil.get("settings")),
      Map(PATH -> "/logout", TEXT -> LanguageUtil.get("logout"))
    ).map { m => {
        if(m.apply(PATH).startsWith(pageBasePath)) {
          m + (SELECT -> SELECT)
        } else {
          m
        }
      }
    }

    val contactGroupKey = request.getParameter("contactGroupKey")
    val subMenu:List[Map[String, String]] = request.getParameter(Constants.KEY_BASE_PATH) match {
      case "/magazine/" =>
        if(contactGroupKey != null){
          List(
            Map(PATH -> "/magazine/index?contactGroupKey=%s".format(contactGroupKey),
                TEXT -> LanguageUtil.get("list"),
                CLASS -> "icon_page"),
            Map(PATH -> "/magazine/form?contactGroupKey=%s".format(contactGroupKey),
                TEXT -> LanguageUtil.get("add"),
                CLASS -> "icon_add")
          )
        } else {
          List()
        }
      case "/stepmail/" => 
        if(contactGroupKey != null){
          List(
            Map(PATH -> "/stepmail/index?contactGroupKey=%s".format(contactGroupKey),
                TEXT -> LanguageUtil.get("list"),
                CLASS -> "icon_page"),
            Map(PATH -> "/stepmail/form?contactGroupKey=%s".format(contactGroupKey),
                TEXT -> LanguageUtil.get("add"),
                CLASS -> "icon_add")
          )
        } else {
          List()
        }
      case "/contact/" => List(
          Map(PATH -> "/contact/group", TEXT -> LanguageUtil.get("list"),
              CLASS -> "icon_page"),
          Map(PATH -> "/contact/groupform", TEXT -> LanguageUtil.get("add"),
              CLASS -> "icon_add")
        )
      case "/reminder/" => List(
          Map(PATH -> "/reminder/index", TEXT -> LanguageUtil.get("list"),
              CLASS -> "icon_page"),
          Map(PATH -> "/reminder/form", TEXT -> LanguageUtil.get("add"),
              CLASS -> "icon_add")
        )
      case _ => List()
    }

    JsObject(List(
        (tojson(MAIN_MENU).asInstanceOf[JsString], tojson(sitemap)),
        (tojson(SUB_MENU).asInstanceOf[JsString], tojson(subMenu))
      ))
    
  }
}
