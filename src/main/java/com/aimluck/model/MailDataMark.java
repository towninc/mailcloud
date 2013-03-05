/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;


import com.google.appengine.api.datastore.Key;
import java.util.Date;
import org.slim3.datastore.ModelRef;

@Model(kind = "mDM", schemaVersion = 1, schemaVersionName = "sV")
/**
 *
 * @author takaseyusuke
 */
public class MailDataMark {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(name = "e")
    private String email;
    @Attribute(name = "sA")
    private Date sentAt;
    @Attribute(name = "sAD")
    private String sentAtDay;
    @Attribute(name = "mT")
    private String mailType;
    @Attribute(name = "mDR")
    private ModelRef<MailData> mailDataRef = new ModelRef<MailData>(MailData.class);
    @Attribute(name = "rGR")
    private ModelRef<RecipientGroup> recipientGroupRef = new ModelRef<RecipientGroup>(RecipientGroup.class);

    /**
     * @return the key
     */
    public Key getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(Key key) {
        this.key = key;
    }

    /**
     * 
     * @return
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     *
     * @return
     */
    public Date getSentAt() {
        return sentAt;
    }

    /**
     * 
     * @param sentAt
     */
    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    /**
     *
     * @return
     */
    public String getSentAtDay() {
        return sentAtDay;
    }

    /**
     * 
     * @param sentAtDay
     */
    public void setSentAtDay(String sentAtDay) {
        this.sentAtDay = sentAtDay;
    }

    /**
     *
     * @return
     */
    public String getMailType() {
        return mailType;
    }

    /**
     * 
     * @param mailType
     */
    public void setMailType(String mailType) {
        this.mailType = mailType;
    }

    /**
     * 
     * @return
     */
    public ModelRef<MailData> getMailDataRef() {
        return mailDataRef;
    }

    /**
     * 
     * @return
     */
    public ModelRef<RecipientGroup> getRecipientGroupRef() {
        return recipientGroupRef;
    }
}
