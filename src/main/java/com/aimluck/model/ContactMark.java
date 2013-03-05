/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;


import com.google.appengine.api.datastore.Key;
import org.slim3.datastore.ModelRef;

@Model(kind = "cM", schemaVersion = 1, schemaVersionName = "sV")
/**
 *
 * @author takaseyusuke
 */
public class ContactMark {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(name = "cR")
    private ModelRef<Contact> contactRef = new ModelRef<Contact>(Contact.class);

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
     * @param ModelRef<Contact>
     */
    public ModelRef<Contact> getContactRef() {
        return contactRef;
    }
}
