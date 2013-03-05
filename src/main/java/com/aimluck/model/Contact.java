/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

/**
 *
 * @author takaseyusuke
 */
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import java.util.Date;
import java.util.List;
import org.slim3.datastore.ModelRef;

@Model(kind = "c", schemaVersion = 1, schemaVersionName = "sV")
public class Contact {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "n")
    private String name;
    @Attribute(name = "e")
    private String email;
    @Attribute(unindexed = true, name = "r")
    private List<String> replacers;
    @Attribute(name = "eGK")
    private String emailGroupKey;
    @Attribute(name = "iS")
    private boolean isSelf;
    @Attribute(name = "cGR")
    private ModelRef<ContactGroup> contactGroupRef = new ModelRef<ContactGroup>(ContactGroup.class);
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "cA")
    private Date createdAt;
    @Attribute(name = "cAD")
    private String createdAtDay;
    @Attribute(name = "cADtz")
    private String createdAtDayWithTimeZone;
    @Attribute(name = "tZ")
    private String timeZone;
    @Attribute(name = "uA")
    private Date updatedAt;

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
    public Long getVersion() {
        return version;
    }

    /**
     *
     * @param version
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
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
    public List<String> getReplacers() {
        return replacers;
    }

    /**
     *
     * @param replacers
     */
    public void setReplacers(List<String> replacers) {
        this.replacers = replacers;
    }

    /**
     *
     * @return
     */
    public String getEmailGroupKey() {
        return emailGroupKey;
    }

    /**
     *
     * @param emailGroupKey
     */
    public void setEmailGroupKey(String emailGroupKey) {
        this.emailGroupKey = emailGroupKey;
    }

    /**
     *
     * @param emailUserId
     */
    public void setEmailGroupKey() {
        ContactGroup group = getContactGroupRef().getModel();
        if (group != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getEmail()).append(":").append(KeyFactory.keyToString(group.getKey()));
            setEmailGroupKey(sb.toString());
        }
    }

    /**
     * 
     * @return
     */
    public boolean isSelf() {
        return isSelf;
    }

    /**
     *
     * @param isSelf
     */
    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    /**
     * 
     * @return
     */
    public ModelRef<ContactGroup> getContactGroupRef() {
        return contactGroupRef;
    }

    /**
     *
     * @return
     */
    public ModelRef<UserData> getUserDataRef() {
        return userDataRef;
    }

    /**
     *
     * @return
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     *
     * @param createdAt
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 
     * @return
     */
    public String getCreatedAtDay() {
        return createdAtDay;
    }

    /**
     * 
     * @param createdAtDay
     */
    public void setCreatedAtDay(String createdAtDay) {
        this.createdAtDay = createdAtDay;
    }

    /**
     * 
     * @return
     */
    public String getCreatedAtDayWithTimeZone() {
        return createdAtDayWithTimeZone;
    }

    /**
     * 
     * @param createdAtDayWithTimeZone
     */
    public void setCreatedAtDayWithTimeZone(String createdAtDayWithTimeZone) {
        this.createdAtDayWithTimeZone = createdAtDayWithTimeZone;
    }

    /**
     *
     * @return
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * 
     * @param timeZone
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     *
     * @return
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     *
     * @param updatedAt
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
