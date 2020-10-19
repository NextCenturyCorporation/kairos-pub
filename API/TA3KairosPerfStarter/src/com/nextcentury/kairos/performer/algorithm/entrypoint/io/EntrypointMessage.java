package com.nextcentury.kairos.performer.algorithm.entrypoint.io;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntrypointMessage {

	@JsonProperty("id")
	private String id;

	@JsonProperty("sender")
	private String sender;

	@JsonProperty("time")
	private String time;

	@JsonProperty("content")
	private Content content;

	@JsonProperty("contentUri")
	private String contentUri;

	public EntrypointMessage() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}



	public String getContentUri() {
		return contentUri;
	}

	public void setContentUri(String contentUri) {
		this.contentUri = contentUri;
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "EntrypointMessage [id=" + id + ", sender=" + sender + ", time=" + time + ", content=" + content
				+ ", contentUri=" + contentUri + "]";
	}

	
}
