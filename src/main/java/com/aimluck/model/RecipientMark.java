/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;


import com.google.appengine.api.datastore.Key;

@Model(kind = "rM", schemaVersion = 1, schemaVersionName = "sV")
/**
 *
 * @author takaseyusuke
 */
public class RecipientMark {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
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
