package com.nextcentury.kairos.statuschecker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nextcentury.kairos.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpExchange;

public class StatusChecker {
	//private static final Logger logger = LogManager.getLogger(StatusChecker.class);

	private final HttpExchange exchange;
	private StatusType statusType;

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE = "Content-Type";

	public enum StatusType {
		READINESS_CHECK, ALIVE_CHECK
	}

	public StatusChecker(HttpExchange exchange, StatusType statusType) {
		this.exchange = exchange;
		this.statusType = statusType;
	}

	public void runStatusCheck() {
		if (statusType == StatusType.ALIVE_CHECK) {
			System.out.println(" - alive check---> " + exchange.getRequestURI().toString());
		} else if (statusType == StatusType.READINESS_CHECK) {
			System.out.println(" - Readiness check---> " + exchange.getRequestURI().toString());
		}

		OutputStream responseBody = null;
		BufferedReader reader = null;
		InputStreamReader isReader = null;
		byte[] response = "success".getBytes();
		try {
			StringBuffer buff = new StringBuffer();
			String line = null;
			isReader = new InputStreamReader(exchange.getRequestBody());
			reader = new BufferedReader(isReader);
			while ((line = reader.readLine()) != null) {
				buff.append(line);
			}

			exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
			// write the response back
			exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
			responseBody = exchange.getResponseBody();
			responseBody.write(response);
		} catch (Throwable e) {
			System.err.println(ExceptionHelper.getExceptionTrace(e));
		} finally {
			if (isReader != null) {
				try {
					isReader.close();
				} catch (Throwable e) {
					System.err.println(ExceptionHelper.getExceptionTrace(e));
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Throwable e) {
					System.err.println(ExceptionHelper.getExceptionTrace(e));
				}
			}
			if (responseBody != null) {
				try {
					responseBody.close();
				} catch (Throwable e) {
					System.err.println(ExceptionHelper.getExceptionTrace(e));
				}
			}

			// free up as much memory as we can
			response = null;
		}
	}
}
