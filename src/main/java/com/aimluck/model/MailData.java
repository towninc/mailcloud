package com.aimluck.model;

import java.util.Date;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import org.slim3.datastore.InverseModelListRef;

@Model(kind = "mD", schemaVersion = 1, schemaVersionName = "sV")
public class MailData {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "s")
    private String subject;
    @Attribute(name = "c", lob=true)
    private String content;
    @Attribute(name = "se")
    private String sender;
    @Attribute(name = "iR")
    private boolean isReplaced;
    @Attribute(persistent = false, name = "rGR")
    private InverseModelListRef<RecipientGroup, MailData> recipientGroupRef =
            new InverseModelListRef<RecipientGroup, MailData>(RecipientGroup.class, "mDR", this);
    @Attribute(name = "st")
    private String status;
    @Attribute(name = "rCA")
    private Long recipientCountAll;
    @Attribute(name = "sC")
    private Long sentCount;
    @Attribute(name = "sA")
    private Date sentAt;
    @Attribute(name = "mT")
    private String mailType;
    @Attribute(name = "n")
    private String note;
    @Attribute(name = "m")
    private String message;
    @Attribute(name = "cA")
    private Date createdAt;
    @Attribute(name = "cAD")
    private String createdAtDay;
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
    public InverseModelListRef<RecipientGroup, MailData> getRecipientGroupRef() {
        return recipientGroupRef;
    }

    /**
     *
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * 
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Not accurately
     * @return
     */
    public Long getSentCount() {
        return sentCount;
    }

    /**
     * 
     * @param sentCount
     */
    public void setSentCount(Long sentCount) {
        this.sentCount = sentCount;
    }

    /**
     * 
     * @return
     */
    public Long getRecipientCountAll() {
        return recipientCountAll;
    }

    /**
     *
     * @param recipientCountAll
     */
    public void setRecipientCountAll(Long recipientCountAll) {
        this.recipientCountAll = recipientCountAll;
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
    public String getNote() {
        return note;
    }

    /**
     * 
     * @param note
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * 
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * 
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
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
