/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.ContactGroupMeta
import com.aimluck.meta.ContactImportLogMeta
import com.aimluck.meta.ContactMarkMeta
import com.aimluck.meta.ContactMeta
import com.aimluck.meta.MailMagazineMeta
import com.aimluck.meta.StepMailMeta
import com.aimluck.model.Contact
import com.aimluck.model.ContactGroup
import com.aimluck.model.ContactImportLog
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import java.util.Date
import java.util.logging.Logger
import org.dotme.liquidtpl.Constants
import org.dotme.liquidtpl.LanguageUtil
import org.dotme.liquidtpl.exception.DataLimitException
import org.dotme.liquidtpl.exception.DuplicateDataException
import org.dotme.liquidtpl.helper.BasicHelper
import org.slim3.datastore.Datastore
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import sjson.json.DefaultProtocol
import sjson.json.Format
import sjson.json.JsonSerialization
import com.aimluck.model.ContactCsv

object ContactGroupService {
  val logger = Logger.getLogger(ContactGroupService.getClass.getName)

  object ContactGroupProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object ContactGroupFormat extends Format[ContactGroup] {
      override def reads(json: JsValue): ContactGroup = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(contactGroup: ContactGroup): JsValue =
        JsObject(List(
          (JsString(Constants.KEY_ID), tojson(if (contactGroup.getKey != null) KeyFactory.keyToString(contactGroup.getKey) else null)),
          (JsString("name"), tojson(contactGroup.getName)),
          (JsString("sampleReplacers"), tojson(if (contactGroup.getSampleReplacers != null) contactGroup.getSampleReplacers.toList else List())),
          (JsString("contactCount"), tojson(contactGroup.getContactCount.intValue)),
          (JsString("isBusy"), tojson(contactGroup.isBusy)),
          (JsString("csvErrors"), tojson(getCsvErrors(contactGroup))),
          (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("contactGroup"), contactGroup.getName)))))))
    }
  }

  object ContactGroupListProtocol extends DefaultProtocol {
    import dispatch.classic.json._
    import JsonSerialization._

    implicit object ContactGroupFormat extends Format[ContactGroup] {
      override def reads(json: JsValue): ContactGroup = json match {
        case _ => throw new IllegalArgumentException
      }

      def writes(contactGroup: ContactGroup): JsValue =
        JsObject(List(
          (JsString(Constants.KEY_ID), tojson(if (contactGroup.getKey != null) KeyFactory.keyToString(contactGroup.getKey) else null)),
          (JsString("name"), tojson(contactGroup.getName)),
          (JsString("sampleReplacers"), tojson(if (contactGroup.getSampleReplacers != null) contactGroup.getSampleReplacers.toList else List())),
          (JsString("contactCount"), tojson(contactGroup.getContactCount.intValue)),
          (JsString("isBusy"), tojson(contactGroup.isBusy)),
          (JsString("csvErrors"), tojson(getCsvErrors(contactGroup))),
          (JsString(Constants.KEY_DELETE_CONFORM), tojson(LanguageUtil.get("deleteOneConform", Some(Array(LanguageUtil.get("contactGroup"), contactGroup.getName)))))))
    }
  }

  def fetchOne(id: String, _userData: Option[UserData]): Option[ContactGroup] = {
    val m: ContactGroupMeta = ContactGroupMeta.get
    val key = KeyFactory.stringToKey(id)
    _userData match {
      case Some(userData) => {
        Datastore.query(m).filter(m.key.equal(key))
          .filter(m.userDataRef.equal(userData.getKey)).asSingle match {
            case v: ContactGroup => Some(v)
            case null => None
          }
      }
      case None => {
        Datastore.query(m).filter(m.key.equal(key)).asSingle match {
          case v: ContactGroup => Some(v)
          case null => None
        }
      }
    }
  }

  def fetchAll(_userData: Option[UserData]): List[ContactGroup] = {
    val m: ContactGroupMeta = ContactGroupMeta.get
    _userData match {
      case Some(userData) => Datastore.query(m)
        .filter(m.userDataRef.equal(userData.getKey)).asList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def fetchFirst(_userData: Option[UserData]): ContactGroup = {
    val m: ContactGroupMeta = ContactGroupMeta.get
    val list: List[ContactGroup] = _userData match {
      case Some(userData) => Datastore.query(m)
        .filter(m.userDataRef.equal(userData.getKey)).limit(1).asList.toList
      case None => Datastore.query(m).limit(1).asList.toList
    }

    if ((list != null) && (list.size > 0)) {
      list.get(0)
    } else {
      null
    }
  }

  def createNew(): ContactGroup = {
    val result: ContactGroup = new ContactGroup
    result.setName("")
    result.setContactCount(0)
    result.setBusy(true)
    result
  }

  def saveWithUserData(model: ContactGroup, userData: UserData): Key = {
    val key: Key = model.getKey
    val oldModel: ContactGroup = try {
      Datastore.get(classOf[ContactGroup], key)
    } catch {
      case e: Exception => model
    }
    val isNew: Boolean = (oldModel == model)

    val now: Date = new Date
    if (model.getCreatedAt == null) {
      model.setTimeZone(AppConstants.timeZone.getID)
      model.setCreatedAt(now)
    }
    model.setUpdatedAt(now)
    model.getUserDataRef.setModel(userData)

    //set sample replacers
    val sampleReplacers = getFirstReplacers(model)
    if (sampleReplacers != null) {
      model.setSampleReplacers(sampleReplacers)
    } else {
      model.setSampleReplacers(List[String]())
    }

    Datastore.put(userData, model).apply(1)
  }

  def cleanByKey(contactGroupKey: String) {
    val key = KeyFactory.stringToKey(contactGroupKey)

    val contactMeta: ContactMeta = ContactMeta.get
    val contactKeyList = Datastore.query(contactMeta)
      .filter(contactMeta.contactGroupRef.equal(key))
      .asKeyList
    if (contactKeyList != null) {
      contactKeyList.foreach { contactKey =>
        val contactMarkMeta: ContactMarkMeta = ContactMarkMeta.get
        Datastore.delete(Datastore.query(contactMarkMeta)
          .filter(contactMarkMeta.contactRef.equal(contactKey))
          .asKeyList)
      }
      Datastore.delete(contactKeyList)
    }

    val stepMailMeta: StepMailMeta = StepMailMeta.get
    Datastore.delete(Datastore.query(stepMailMeta)
      .filter(stepMailMeta.contactGroupRef.equal(key))
      .asKeyList)

    val mailMagazineMeta: MailMagazineMeta = MailMagazineMeta.get
    Datastore.delete(Datastore.query(mailMagazineMeta)
      .filter(mailMagazineMeta.contactGroupRef.equal(key))
      .asKeyList)
  }

  def delete(contactGroup: ContactGroup) {
    Datastore.delete(contactGroup.getKey)
  }

  def getFirstReplacers(contactGroup: ContactGroup): List[String] = {
    ContactService.fetchFirst(contactGroup) match {
      case null => null.asInstanceOf[List[String]]
      case first: Contact => first.getReplacers.toList
    }
  }

  def getImportLogs(contactCsv: ContactCsv): List[ContactImportLog] = {
    val m: ContactImportLogMeta = ContactImportLogMeta.get
    if (contactCsv != null) {
      asScalaBuffer(Datastore.query(m).filter(m.contactCsvRef.equal(contactCsv.getKey)).asList).toList
    } else {
      List[ContactImportLog]()
    }
  }

  def getOverwrittenCount(contactCsv: ContactCsv): Int = {
    getOverwrittenCount(getImportLogs(contactCsv))
  }

  def getOverwrittenCount(importLogs: List[ContactImportLog]): Int = {
    try {
      importLogs.foldLeft(0) { (x, y) =>
        x + y.getOverwrittenCount()
      }
    } catch {
      case _ => 0
    }
  }

  def getErrorLines(contactCsv: ContactCsv): List[String] = {
    getErrorLines(getImportLogs(contactCsv))
  }

  def getErrorLines(importLogs: List[ContactImportLog]): List[String] = {
    try {
      importLogs.flatMap { e =>
        e.getErrorLines()
      }
    } catch {
      case _ => List[String]()
    }
  }

  def getLineCount(contactCsv: ContactCsv): Int = {
    getLineCount(getImportLogs(contactCsv))
  }

  def getLineCount(importLogs: List[ContactImportLog]): Int = {
    try {
      importLogs.foldLeft(0) { (x, y) =>
        x + y.getLineCount()
      }
    } catch {
      case _ => 0
    }
  }

  def getContactCsv(contactGroup: ContactGroup): ContactCsv = {
    val contactCsvList = asScalaBuffer(contactGroup.getContactCsvRef().getModelList()).toList.sortWith {
      (x, y) =>
        x.getCreatedAt().before(y.getCreatedAt())
    }

    if (contactCsvList != null && contactCsvList.size > 0) {
      contactCsvList.apply(0)
    } else {
      null
    }
  }

  def getCsvErrors(contactGroup: ContactGroup): List[String] = {
    val contactCsvList = asScalaBuffer(contactGroup.getContactCsvRef().getModelList()).toList.sortWith {
      (x, y) =>
        x.getCreatedAt().before(y.getCreatedAt())
    }

    if (contactCsvList != null && contactCsvList.size > 0) {
      val importLogs = getImportLogs(contactCsvList.apply(0))
      val errorLines: List[String] = getErrorLines(importLogs)
      val errorList: List[String] = if (errorLines != null) {
        asScalaBuffer(errorLines).map { error =>
          if (error.indexOf(":") > 0) {
            val email = error.split(":").apply(0)
            val etype = error.split(":").apply(1)
            val errorMap: (String, String) = ContactCsvService.errorTypeMap.find(e => e._1 == etype).getOrElse(null)
            if (errorMap != null) {
              LanguageUtil.get(errorMap._2, Some(Array(email)))
            } else {
              error
            }
          } else {
            error
          }
        }.toList
      } else {
        List[String]()
      }

      try {
        val overwrittenCount: Int = getOverwrittenCount(importLogs)
        if (overwrittenCount > 0) {
          errorList ::: List[String](LanguageUtil.get("contactCsv.overwrittenLineCount",
            Some(Array(Integer.toString(overwrittenCount)))))
        } else {
          errorList
        }
      } catch {
        case _ => errorList
      }

    } else {
      List[String]()
    }
  }

}
