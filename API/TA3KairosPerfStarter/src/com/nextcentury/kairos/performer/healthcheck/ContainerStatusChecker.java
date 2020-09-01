package com.nextcentury.kairos.performer.healthcheck;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nextcentury.kairos.utils.ExceptionHelper;
import com.nextcentury.kairos.utils.StatusCode;
import com.sun.net.httpserver.HttpExchange;

public class ContainerStatusChecker {
	private static final Logger logger = LogManager.getLogger(ContainerStatusChecker.class);

	private HttpExchange exchange;
	private StatusType statusType;

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE = "Content-Type";

	public enum StatusType {
		READINESS_CHECK, ALIVE_CHECK, STARTUP_CHECK
	};

	public ContainerStatusChecker(HttpExchange exchange, StatusType statusType) {
		this.exchange = exchange;
		this.statusType = statusType;
	}

	public void runStatusCheck() {
		if (statusType == StatusType.ALIVE_CHECK) {
			logger.debug(" - Alive check---> " + exchange.getRequestURI().toString());
		} else if (statusType == StatusType.READINESS_CHECK) {
			logger.debug(" - Readiness check---> " + exchange.getRequestURI().toString());
		} else if (statusType == StatusType.STARTUP_CHECK) {
			logger.debug(" - Startup check---> " + exchange.getRequestURI().toString());
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

			// send back status code to specify check result probe
			exchange.sendResponseHeaders(StatusCode.OK.getCode(), 0);

			// write the response back
			exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
			responseBody = exchange.getResponseBody();
			responseBody.write(response);
		} catch (Throwable e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		} finally {
			if (isReader != null) {
				try {
					isReader.close();
				} catch (Throwable e) {
					logger.error(ExceptionHelper.getExceptionTrace(e));
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Throwable e) {
					logger.error(ExceptionHelper.getExceptionTrace(e));
				}
			}
			if (responseBody != null) {
				try {
					responseBody.close();
				} catch (Throwable e) {
					logger.error(ExceptionHelper.getExceptionTrace(e));
				}
			}

			// free up as much memory as we can
			response = null;
		}
	}
}
