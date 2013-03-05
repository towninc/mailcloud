/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;


import com.google.appengine.api.datastore.Key;
import java.util.Date;
import java.util.List;
import org.slim3.datastore.ModelRef;

@Model(kind = "mDEM", schemaVersion = 1, schemaVersionName = "sV")
/**
 *
 * @author takaseyusuke
 */
public class MailDataErrorMark {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(name = "e")
    private String email;
    @Attribute(name = "eA")
    private Date errorAt;
    @Attribute(name = "eAD")
    private String errorAtDay;
    @Attribute(name = "m")
    private String message;
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
    public Date getErrorAt() {
        return errorAt;
    }

    /**
     *
     * @param errorAt
     */
    public void setErrorAt(Date errorAt) {
        this.errorAt = errorAt;
    }

    /**
     *
     * @return
     */
    public String getErrorAtDay() {
        return errorAtDay;
    }

    /**
     * 
     * @param errorAtDay
     */
    public void setErrorAtDay(String errorAtDay) {
        this.errorAtDay = errorAtDay;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
