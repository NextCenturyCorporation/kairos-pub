package com.nextcentury.kairos.restclient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public abstract class AbstractHttpClient {
	protected CloseableHttpClient httpClient = null;
	private static PoolingHttpClientConnectionManager cm = null;

	public AbstractHttpClient() {
		super();

		if (cm != null) {
			cm = new PoolingHttpClientConnectionManager();
			// cm.setDefaultMaxPerRoute(40);
			cm.setMaxTotal(100);
		}
	}

	protected CloseableHttpClient getHttpClient() {
		// httpClient = HttpClients.createMinimal(cm);
		httpClient = HttpClients.custom().setConnectionManager(cm).build();
		return httpClient;
	}

	protected void closeHttpClient() {
		if (httpClient != null) {
			try {
				httpClient.close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
