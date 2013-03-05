package com.aimluck.model;

import java.util.Date;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;

@Model(kind = "mM", schemaVersion = 1, schemaVersionName = "sV")
public class MailMagazine {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "s")
    private String subject;
    @Attribute(name = "c", lob = true)
    private String content;
    @Attribute(name = "se")
    private String sender;
    @Attribute(name = "iR")
    private boolean isReplaced;
    @Attribute(name = "cGR")
    private ModelRef<ContactGroup> contactGroupRef = new ModelRef<ContactGroup>(ContactGroup.class);
    @Attribute(name = "iA")
    private boolean isActive;
    @Attribute(name = "sDT")
    private Date sendDateTime;
    @Attribute(name = "lSA")
    private Date lastSentAt;
    @Attribute(name = "cA")
    private Date createdAt;
    @Attribute(name = "cAD")
    private String createdAtDay;
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
    public ModelRef<UserData> getUserDataRef() {
        return userDataRef;
    }

    /**
     *
     * @return
     */
    public String getSubject() {
        return subject;
    }

    /**
     *
     * @param subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     *
     * @return
     */
    public String getContent() {
        return content;
    }

    /**
     *
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     *
     * @return
     */
    public String getSender() {
        return sender;
    }

    /**
     *
     * @param sender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     *
     * @return
     */
    public boolean isReplaced() {
        return isReplaced;
    }

    /**
     * 
     * @param isReplaced
     */
    public void setReplaced(boolean isReplaced) {
        this.isReplaced = isReplaced;
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
    public boolean isActive() {
        return isActive;
    }

    /**
     *
     * @param isActive
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * 
     * @return
     */
    public Date getSendDateTime() {
        return sendDateTime;
    }

    /**
     * 
     * @param sendTime
     */
    public void setSendDateTime(Date sendDateTime) {
        this.sendDateTime = sendDateTime;
    }

    /**
     *
     * @return
     */
    public Date getLastSentAt() {
        return lastSentAt;
    }

    /**
     * 
     * @param lastSentAt
     */
    public void setLastSentAt(Date lastSentAt) {
        this.lastSentAt = lastSentAt;
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
