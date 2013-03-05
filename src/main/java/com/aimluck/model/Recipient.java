/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aimluck.model;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;


import com.google.appengine.api.datastore.Key;
import java.util.List;
import org.slim3.datastore.ModelRef;

@Model(kind = "r", schemaVersion = 1, schemaVersionName = "sV")
/**
 *
 * @author takaseyusuke
 */
public class Recipient {

    @Attribute(primaryKey = true, name = "k")
    private Key key;
    @Attribute(version = true, name = "v")
    private Long version;
    @Attribute(name = "e")
    private String email;
    @Attribute(unindexed = true, name = "r")
    private List<String> replacers;
    @Attribute(name = "rGR")
    private ModelRef<RecipientGroup> recipientGroupRef = new ModelRef<RecipientGroup>(RecipientGroup.class);

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
    public ModelRef<RecipientGroup> getRecipientGroupRef() {
        return recipientGroupRef;
    }
}
