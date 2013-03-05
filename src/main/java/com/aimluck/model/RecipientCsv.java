package com.aimluck.model;

import java.util.Date;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import java.util.List;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Sort;

@Model(kind = "rCsv", schemaVersion = 1, schemaVersionName = "sV")
public class RecipientCsv {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "uDR")
    private ModelRef<UserData> userDataRef = new ModelRef<UserData>(UserData.class);
    @Attribute(name = "mDR")
    private ModelRef<MailData> mailDataRef = new ModelRef<MailData>(MailData.class);
    @Attribute(name = "cs")
    private String charset;
    @Attribute(name = "l")
    private int length;
    @Attribute(persistent = false, name = "rCFR")
    private InverseModelListRef<RecipientCsvFragment, RecipientCsv> recipientCsvFragmentRef =
            new InverseModelListRef<RecipientCsvFragment, RecipientCsv>(RecipientCsvFragment.class, "rCR", this, new Sort("i"));
    @Attribute(unindexed = true, name = "sR")
    private List<String> sampleReplacers;
    @Attribute(name = "cA")
    private Date createdAt;
    @Attribute(name = "cAD")
    private String createdAtDay;

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
    public InverseModelListRef<RecipientCsvFragment, RecipientCsv> getRecipientCsvFragmentRef() {
        return recipientCsvFragmentRef;
    }

    /**
     *
     * @return
     */
    public String getCharset() {
        return charset;
    }

    /**
     * 
     * @param charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     *
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     * 
     * @param length
     */
    public void setLength(int length) {
        this.length = length;
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
}
