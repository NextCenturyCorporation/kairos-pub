package com.nextcentury.kairos.performer.algorithm.status;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.nextcentury.kairos.performer.algorithm.status.io.PerformerStatusResponseType;
import com.nextcentury.kairos.performer.algorithm.status.io.StatusResponse;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpExchange;

public class AlgorithmStatusChecker {
	private static final Logger logger = LogManager.getLogger(AlgorithmStatusChecker.class);

	private HttpExchange exchange;

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE = "Content-Type";

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public AlgorithmStatusChecker(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public void runStatusCheck() {
		logger.debug(" - Performer Status check---> " + exchange.getRequestURI().toString());

		OutputStream responseBody = null;
		BufferedReader reader = null;
		InputStreamReader isReader = null;

		try {
			StringBuffer buff = new StringBuffer();
			String line = null;
			isReader = new InputStreamReader(exchange.getRequestBody());
			reader = new BufferedReader(isReader);
			while ((line = reader.readLine()) != null) {
				buff.append(line);
			}

			StatusResponse statusResponse = new StatusResponse();
			statusResponse.setStatus(PerformerStatusResponseType.INITIALIZING);
			String payloadStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusResponse);

			// send back status code to specify check result probe
			exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
			// write the response back
			exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
			responseBody = exchange.getResponseBody();
			responseBody.write(payloadStr.getBytes());
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
		}
	}
}
