/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.aimluck.service


import au.com.bytecode.opencsv.CSVReader
import com.aimluck.lib.util.AppConstants
import com.aimluck.meta.RecipientCsvFragmentMeta
import com.aimluck.meta.RecipientCsvMeta
import com.aimluck.model.MailData
import com.aimluck.model.RecipientCsv
import com.aimluck.model.RecipientCsvFragment
import com.aimluck.model.RecipientGroup
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

object RecipientCsvService {
  val logger = Logger.getLogger(UserDataService.getClass.getName)
  val FRAGMENT_SIZE:Int = 524288
  val d:RecipientCsvMeta = RecipientCsvMeta.get();
  val f:RecipientCsvFragmentMeta = RecipientCsvFragmentMeta.get();

  @throws(classOf[Exception])
  def upload(formFile:FileItem, charset:String):RecipientCsv = {
    if (formFile == null) {
      return null;
    }

    val now:Date = new Date
    val models:ListBuffer[Object] = ListBuffer[Object]()

    val data:RecipientCsv = new RecipientCsv;
    data.setKey(Datastore.allocateId(classOf[RecipientCsv]))
    data.setCharset(charset)
    data.setCreatedAt(now)
    data.setCreatedAtDay(AppConstants.dayCountFormat.format(now))

    //read 1st line
    val reader = new java.io.InputStreamReader(
      new java.io.ByteArrayInputStream(formFile.getData), charset);
    try {
      val csvReader:CSVReader = new CSVReader(reader);
      var nextLine:Array[String] = csvReader.readNext();
      if((nextLine != null) && (nextLine.size > 0)){
        data.setSampleReplacers(nextLine.toList)
      }
    } catch {
      case e:Exception => throw e
    } finally {
      reader.close
    }

    data.setLength(formFile.getData.length);
    val Bytes:Array[Byte] = formFile.getData;
    val BytesArray:Array[Array[Byte]] = ByteUtil.split(Bytes, FRAGMENT_SIZE);
    val keys:Iterator[Key] = Datastore
    .allocateIds(data.getKey, f, BytesArray.length).iterator;

    val length:Int = BytesArray.size
    for (i <- (0 to length - 1) ) {
      val fragmentData:Array[Byte] = BytesArray.apply(i);
      val fragment:RecipientCsvFragment = new RecipientCsvFragment;
      models.append(fragment);
      fragment.setKey(keys.next);
      fragment.setBytes(fragmentData);
      fragment.setIndex(i);
      fragment.getRecipientCsvRef.setModel(data);
    }
    val tx:Transaction = Datastore.beginTransaction;
    Datastore.put(tx, data);
    models.toList.foreach { model =>
      Datastore.put(tx, model);
    }
    tx.commit;
    return data;
  }

  def getData( key:Key, version:Long):RecipientCsv = {
    return Datastore.get(d, key, version);
  }

  def getBytes(RecipientCsv:RecipientCsv):Array[Byte] = {
    if (RecipientCsv == null) {
      throw new NullPointerException(
        "The RecipientCsv parameter must not be null.");
    }
    val fragmentList:List[RecipientCsvFragment] =
      RecipientCsv.getRecipientCsvFragmentRef.getModelList.toList

    val BytesArray:Array[Array[Byte]] =  fragmentList.map{
      _.getBytes;
    }.toArray
    return ByteUtil.join(BytesArray);
  }

  def fetchAll(_userData:Option[UserData]):List[RecipientCsv] = {
    val m:RecipientCsvMeta = RecipientCsvMeta.get
    _userData match {
      case Some(userData) =>
        Datastore.query(m).filter(m.userDataRef.equal(userData.getKey)).asList.toList
      case None => Datastore.query(m).asList.toList
    }
  }

  def fetchOne( id:String):Option[RecipientCsv] = {
    val m:RecipientCsvMeta = RecipientCsvMeta.get
    try {
      val key = KeyFactory.stringToKey(id)
      Datastore.query(m).filter(m.key.equal(key)).asSingle match {
        case v:RecipientCsv => Some(v)
        case null => None
      }
    } catch {
      case e:Exception => {
          logger.severe(e.getMessage)
          logger.severe(e.getStackTraceString)
          None
        }
    }
  }

  def fetchOne( mailData:MailData):Option[RecipientCsv] = {
    val m:RecipientCsvMeta = RecipientCsvMeta.get
    try {
      Datastore.query(m).filter(m.mailDataRef.equal(mailData.getKey)).asSingle match {
        case v:RecipientCsv => Some(v)
        case null => None
      }
    } catch {
      case e:Exception => {
          logger.severe(e.getMessage)
          logger.severe(e.getStackTraceString)
          None
        }
    }
  }

  def saveWithUserData(model:RecipientCsv, userData:UserData):Key = {
    saveWithMailData(model, userData, None)
  }

  def saveWithMailData(model:RecipientCsv, mailData:MailData):Key = {
    saveWithMailData(model, mailData.getUserDataRef.getModel, Some(mailData))
  }

  def saveWithMailData(model:RecipientCsv, userData:UserData, _mailData:Option[MailData]):Key = {
    val key:Key = model.getKey
    val now:Date = new Date
    if(model.getCreatedAt == null){
      model.setCreatedAt(now)
      model.setCreatedAtDay(AppConstants.dayCountFormat.format(now))
    }
    model.getUserDataRef.setModel(userData)
    _mailData match {
      case Some(mailData) => {
          model.getMailDataRef.setModel(mailData)
          Datastore.put(userData, mailData, model).apply(2)
        }
      case None => Datastore.put(userData, model).apply(1)
    }
  }

  def delete(model:RecipientCsv) = {
    val key:Key = model.getKey
    val tx:Transaction = Datastore.beginTransaction;
    val keys:ListBuffer[Key] = ListBuffer[Key]();
    keys.append(key);
    keys.appendAll(Datastore.query(f, key).asKeyList);
    Datastore.delete(tx, keys.toList);
    tx.commit;
  }

}
