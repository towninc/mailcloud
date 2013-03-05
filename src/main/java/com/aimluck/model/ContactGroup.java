/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

/**
 *
 * @author takaseyusuke
 */
import java.util.Date;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;

@Model(kind = "cG", schemaVersion = 1, schemaVersionName = "sV")
public class ContactGroup {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "n")
    private String name;
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "cCsvR", persistent = false)
    private InverseModelListRef<ContactCsv, ContactGroup> contactCsvRef =
            new InverseModelListRef<ContactCsv, ContactGroup>(ContactCsv.class, "cGR", this);
    @Attribute(unindexed = true, name = "sR")
    private List<String> sampleReplacers;
    @Attribute(name = "iB")
    private boolean isBusy;
    @Attribute(name = "cC")
    private Long contactCount;
    @Attribute(name = "cA")
    private Date createdAt;
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
    public ModelRef<UserData> getUserDataRef() {
        return userDataRef;
    }

    /**
     * 
     * @return
     */
    public InverseModelListRef<ContactCsv, ContactGroup> getContactCsvRef() {
        return contactCsvRef;
    }

    /**
     *
     * @return
     */
    public List<String> getSampleReplacers() {
        return sampleReplacers;
    }

    /**
     *
     * @param sampleReplacers
     */
    public void setSampleReplacers(List<String> sampleReplacers) {
        this.sampleReplacers = sampleReplacers;
    }

    /**
     * 
     * @return
     */
    public boolean isBusy() {
        return isBusy;
    }

    /**
     * 
     * @param isBusy
     */
    public void setBusy(boolean isBusy) {
        this.isBusy = isBusy;
    }

    /**
     * 
     * @return
     */
    public Long getContactCount() {
        return contactCount;
    }

    /**
     * 
     * @param contactCount
     */
    public void setContactCount(Long contactCount) {
        this.contactCount = contactCount;
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
