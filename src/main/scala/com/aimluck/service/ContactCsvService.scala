/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service

import au.com.bytecode.opencsv.CSVReader
import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.ContactCsvFragmentMeta
import com.aimluck.meta.ContactCsvMeta
import com.aimluck.model.ContactGroup
import com.aimluck.model.ContactCsv
import com.aimluck.model.ContactCsvFragment
import com.aimluck.model.ContactGroup
import com.aimluck.model.UserData
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.datastore.Transaction
import java.util.Date
import java.util.logging.Logger
import org.slim3.controller.upload.FileItem
import org.slim3.datastore.Datastore
import org.slim3.util.ByteUtil
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import org.dotme.liquidtpl.LanguageUtil

object ContactCsvService {
  val logger = Logger.getLogger(UserDataService.getClass.getName)
  val FRAGMENT_SIZE: Int = 524288
  val d: ContactCsvMeta = ContactCsvMeta.get();
  val f: ContactCsvFragmentMeta = ContactCsvFragmentMeta.get();

  object ErrorType extends Enumeration {
    val DUPLICATED = Value("D")
    val INVALID_EMAIL = Value("I")
  }

  val errorTypeMap: List[(String, String)] = List[(String, String)](
    ErrorType.DUPLICATED.toString -> "contactCsv.ErrorType.duplicated",
    ErrorType.INVALID_EMAIL.toString -> "contactCsv.ErrorType.invalidEmail")

  @throws(classOf[Exception])
  def upload(formFile: FileItem, charset: String): ContactCsv = {
    if (formFile == null) {
      return null;
    }

    val now: Date = new Date
    val models: ListBuffer[Object] = ListBuffer[Object]()

    val data: ContactCsv = new ContactCsv;
    data.setKey(Datastore.allocateId(classOf[ContactCsv]))
    data.setCharset(charset)
    data.setCreatedAt(now)
    data.setCreatedAtDay(AppConstants.dayCountFormat.format(now))

    //read 1st line
    val reader = new java.io.InputStreamReader(
      new java.io.ByteArrayInputStream(formFile.getData), charset);
    try {
      val csvReader: CSVReader = new CSVReader(reader);
      var nextLine: Array[String] = csvReader.readNext();
      if ((nextLine != null) && (nextLine.size > 0)) {
        data.setSampleReplacers(nextLine.toList)
      }
    } catch {
      case e: Exception => throw e
    } finally {
      reader.close
    }

    data.setLength(formFile.getData.length);
    val Bytes: Array[Byte] = formFile.getData;
    val BytesArray: Array[Array[Byte]] = ByteUtil.split(Bytes, FRAGMENT_SIZE);
    val keys: Iterator[Key] = Datastore
      .allocateIds(data.getKey, f, BytesArray.length).iterator;

    val length: Int = BytesArray.size
    for (i <- (0 to length - 1)) {
      val fragmentData: Array[Byte] = BytesArray.apply(i);
      val fragment: ContactCsvFragment = new ContactCsvFragment;
      models.append(fragment);
      fragment.setKey(keys.next);
      fragment.setBytes(fragmentData);
      fragment.setIndex(i);
      fragment.getContactCsvRef.setModel(data);
    }
    val tx: Transaction = Datastore.beginTransaction;
    Datastore.put(tx, data);
    models.toList.foreach { model =>
      Datastore.put(tx, model);
    }
    tx.commit;
    return data;
  }

  def getData(key: Key, version: Long): ContactCsv = {
    return Datastore.get(d, key, version);
  }

  def getBytes(ContactCsv: ContactCsv): Array[Byte] = {
    if (ContactCsv == null) {
      throw new NullPointerException(
        "The ContactCsv parameter must not be null.");
    }
    val fragmentList: List[ContactCsvFragment] =
      ContactCsv.getContactCsvFragmentRef.getModelList.toList

    val BytesArray: Array[Array[Byte]] = fragmentList.map {
      _.getBytes;
    }.toArray
    return ByteUtil.join(BytesArray);
  }

  def fetchAll(_userData: Option[UserData]): List[ContactCsv] = {
    val m: ContactCsvMeta = ContactCsvMeta.get
    _userData match {
      case Some(userData) =>
        Datastore.query(m).filter(m.userDataRef.equal(userData.getKey)).asList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def fetchOne(id: String): Option[ContactCsv] = {
    val m: ContactCsvMeta = ContactCsvMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      Datastore.query(m).filter(m.key.equal(key)).asSingle match {
        case v: ContactCsv => Some(v)
        case null => None
      }
    } catch {
      case e: Exception => {
        logger.severe(e.getMessage)
        logger.severe(e.getStackTraceString)
        None
      }
    }
  }

  def fetchOne(contactGroup: ContactGroup): Option[ContactCsv] = {
    val m: ContactCsvMeta = ContactCsvMeta.get
    try {
      Datastore.query(m).filter(m.contactGroupRef.equal(contactGroup.getKey)).asSingle match {
        case v: ContactCsv => Some(v)
        case null => None
      }
    } catch {
      case e: Exception => {
        logger.severe(e.getMessage)
        logger.severe(e.getStackTraceString)
        None
      }
    }
  }

  def saveWithUserData(model: ContactCsv, userData: UserData): Key = {
    saveWithContactGroup(model, userData, None)
  }

  def saveWithContactGroup(model: ContactCsv, contactGroup: ContactGroup): Key = {
    saveWithContactGroup(model, contactGroup.getUserDataRef.getModel, Some(contactGroup))
  }

  def saveWithContactGroup(model: ContactCsv, userData: UserData, _contactGroup: Option[ContactGroup]): Key = {
    val key: Key = model.getKey
    val now: Date = new Date
    if (model.getCreatedAt == null) {
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
    }
    model.getUserDataRef.setModel(userData)
    _contactGroup match {
      case Some(contactGroup) => {
        model.getContactGroupRef.setModel(contactGroup)
        println("contactGroup: %s".format(contactGroup))
        println("userData: %s".format(userData))
        Datastore.put(userData, contactGroup, model).apply(2)
      }
      case None => Datastore.put(userData, model).apply(1)
    }
  }

  def delete(model: ContactCsv) = {
    val key: Key = model.getKey
    val tx: Transaction = Datastore.beginTransaction;
    val keys: ListBuffer[Key] = ListBuffer[Key]();
    ContactGroupService.getImportLogs(model).foreach { importLog =>
      keys.append(importLog.getKey());
    }
    model.getContactCsvFragmentRef().getModelList().foreach { fragment =>
      keys.append(fragment.getKey());
    }
    keys.append(key);
    keys.appendAll(Datastore.query(f, key).asKeyList);
    Datastore.delete(tx, keys.toList);
    tx.commit;
  }

}
