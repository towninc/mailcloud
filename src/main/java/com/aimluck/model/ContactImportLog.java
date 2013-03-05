package com.aimluck.model;

import java.util.Date;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;
import com.google.appengine.api.datastore.Key;

@Model(kind = "cIL", schemaVersion = 1, schemaVersionName = "sV")
public class ContactImportLog {

	@Attribute(primaryKey = true, name = "k")
	private Key key;
	@Attribute(version = true, name = "v")
	private Long version;
	@Attribute(name = "uDR")
	private ModelRef<UserData> userDataRef = new ModelRef<UserData>(
			UserData.class);
	@Attribute(name = "cGR")
	private ModelRef<ContactGroup> contactGroupRef = new ModelRef<ContactGroup>(
			ContactGroup.class);
	@Attribute(name = "cCsvR")
	private ModelRef<ContactCsv> contactCsvRef = new ModelRef<ContactCsv>(
			ContactCsv.class);
	@Attribute(name = "c", lob = true)
	private String content;
	@Attribute(unindexed = true, name = "eL")
	private List<String> errorLines;
	@Attribute(unindexed = true, name = "oC")
	private int overwrittenCount;
	@Attribute(name = "cA")
	private Date createdAt;
	@Attribute(name = "lC")
	private int lineCount;

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
	public ModelRef<ContactGroup> getContactGroupRef() {
		return contactGroupRef;
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

	public List<String> getErrorLines() {
		return errorLines;
	}

	public void setErrorLines(List<String> errorLines) {
		this.errorLines = errorLines;
	}

	public int getOverwrittenCount() {
		return overwrittenCount;
	}

	public void setOverwrittenCount(int overwritternCount) {
		this.overwrittenCount = overwritternCount;
	}

	public ModelRef<ContactCsv> getContactCsvRef() {
		return contactCsvRef;
	}
	
	public int getLineCount() {
		return lineCount;
	}

	public void setLineCount(int lineCount) {
		this.lineCount = lineCount;
	}
}
