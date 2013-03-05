package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;

@Model(kind = "rCsvF", schemaVersion = 1, schemaVersionName = "sV")
public class RecipientCsvFragment {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(lob = true, name = "b")
    private byte[] bytes;
    @Attribute(name = "i")
    private int index;
    @Attribute(name = "rCR")
    private ModelRef<RecipientCsv> recipientCsvRef =
            new ModelRef<RecipientCsv>(RecipientCsv.class);

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
     * @return the array of bytes
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * @param bytes
     *            the array of bytes
     */
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * @param index
     *            the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * 
     * @return
     */
    public ModelRef<RecipientCsv> getRecipientCsvRef() {
        return recipientCsvRef;
    }
}
