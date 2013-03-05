package com.aimluck.model;

import java.util.Date;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import java.util.List;
import org.slim3.datastore.InverseModelListRef;

@Model(kind = "re", schemaVersion = 1, schemaVersionName = "sV")
public class Reminder {

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
    @Attribute(name = "rT", lob = true)
    private String repeatType;
    @Attribute(name = "rC")
    private int repeatCycle;
    @Attribute(name = "rWD")
    private int repeatWeekDays;
    @Attribute(name = "sD")
    private Date startDate;
    @Attribute(name = "eD")
    private Date endDate;
    @Attribute(name = "iE")
    private boolean isEnd;
    @Attribute(name = "lSA")
    private Date lastSentAt;
    @Attribute(unindexed = true, name = "r")
    private List<String> recipients;
    @Attribute(name = "cA")
    private Date createdAt;
    @Attribute(name = "cAD")
    private String createdAtDay;
    @Attribute(name = "uA")
    private Date updatedAt;
    @Attribute(name = "tZ")
    private String timeZone;

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
    public String getRepeatType() {
        return repeatType;
    }

    /**
     *
     * @param repeatType
     */
    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    /**
     *
     * @return
     */
    public int getRepeatCycle() {
        return repeatCycle;
    }

    /**
     *
     * @param repeatCycle
     */
    public void setRepeatCycle(int repeatCycle) {
        this.repeatCycle = repeatCycle;
    }

    /**
     *
     * @return
     */
    public int getRepeatWeekDays() {
        return repeatWeekDays;
    }

    /**
     *
     * @param repeatWeekDays
     */
    public void setRepeatWeekDays(int repeatWeekDays) {
        this.repeatWeekDays = repeatWeekDays;
    }

    /**
     *
     * @return
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     *
     * @param startDate
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     *
     * @return
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     *
     * @param endDate
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     *
     * @return
     */
    public boolean isEnd() {
        return isEnd;
    }

    /**
     *
     * @param isEnd
     */
    public void setEnd(boolean isEnd) {
        this.isEnd = isEnd;
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
    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * 
     * @param recipients
     */
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
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
}
