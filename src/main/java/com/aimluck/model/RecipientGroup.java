package com.aimluck.model;

import java.util.Date;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import java.util.List;
import org.slim3.datastore.InverseModelRef;

@Model(kind = "rG", schemaVersion = 1, schemaVersionName = "sV")
public class RecipientGroup {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "mDR")
    private ModelRef<MailData> mailDataRef = new ModelRef<MailData>(MailData.class);
    @Attribute(name = "rCsvR", persistent = false)
    private InverseModelRef<RecipientCsv, RecipientGroup> recipientCsvRef =
            new InverseModelRef<RecipientCsv, RecipientGroup>(RecipientCsv.class, "rGR", this);
    @Attribute(name = "rC")
    private Long recipientCount;
    @Attribute(name = "cAD")
    private String createdAtDay;
    @Attribute(name = "cA")
    private Date createdAt;

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
    public ModelRef<MailData> getMailDataRef() {
        return mailDataRef;
    }

    /**
     * 
     * @return
     */
    public InverseModelRef<RecipientCsv, RecipientGroup> getRecipientCsvRef() {
        return recipientCsvRef;
    }

    /**
     * 
     * @return
     */
    public Long getRecipientCount() {
        return recipientCount;
    }

    /**
     * 
     * @param recipientsCount
     */
    public void setRecipientCount(Long recipientsCount) {
        this.recipientCount = recipientsCount;
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
}
