package com.nextcentury.kairos.restclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestClient extends AbstractHttpClient {
	private String url;
	private String textData;
	private int statusCode = -1;

	private static final Logger logger = LogManager.getLogger(RestClient.class);

	public RestClient(String url, String textData) {
		super();
		this.url = url;
		this.textData = textData;
	}

	public String getResponse() {
		return execute();
	}

	public int getStatusCode() {
		return statusCode;
	}

	private String execute() {
		StringBuffer buff = new StringBuffer();
		CloseableHttpResponse response = null;
		HttpEntity entity = null;

		BufferedReader reader = null;
		try {
			HttpPost httpPost = new HttpPost(this.url);
			httpPost.setEntity(new StringEntity(textData, ContentType.APPLICATION_JSON));
			// logger.debug("Executing http post");
			response = getHttpClient().execute(httpPost);
			// logger.debug("returning from http call");
			entity = response.getEntity();

			statusCode = response.getStatusLine().getStatusCode();
			logger.debug("Status code: " + statusCode);

			String line = null;
			reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			while ((line = reader.readLine()) != null) {
				buff.append(line);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				if (entity != null)
					EntityUtils.consume(entity);
				if (response != null)
					response.close();

				closeHttpClient();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return buff.toString();
	}
}