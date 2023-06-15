package com.nextcentury.restclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RestClient extends AbstractHttpClient {
	private static final Logger logger = LogManager.getLogger(RestClient.class);
	
	public RestClient() {
		super();
	}

	public RestClientResponse get(String url) {
		CloseableHttpResponse response = null;
		HttpEntity entity = null;

		BufferedReader reader = null;
		RestClientResponse restClientResponse = null;
		StringBuffer buff = new StringBuffer();

		try {
			HttpGet httpGet = new HttpGet(url);

			logger.debug("Executing http get - " + url);
			response = getHttpClient().execute(httpGet);
			logger.debug("returning from http call");

			entity = response.getEntity();

			int statusCode = response.getStatusLine().getStatusCode();
			logger.debug("Status code: " + statusCode);

			reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			String line;
			while ((line = reader.readLine()) != null) {
				buff.append(line);
			}

			restClientResponse = new RestClientResponse(buff.toString(), statusCode);
		} catch (Throwable e) {
			restClientResponse = new RestClientResponse(buff.toString(), HttpStatus.SC_BAD_REQUEST);
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
		return restClientResponse;
	}
}