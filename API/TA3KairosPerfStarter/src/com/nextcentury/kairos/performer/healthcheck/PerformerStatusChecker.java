package com.nextcentury.kairos.performer.healthcheck;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.nextcentury.kairos.tuple.PerformerStatusCheckTuple;
import com.nextcentury.kairos.tuple.PerformerStatusCheckTuple.PerformerStatusType;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.nextcentury.kairos.utils.StatusCode;
import com.sun.net.httpserver.HttpExchange;

public class PerformerStatusChecker {
	private static final Logger logger = LogManager.getLogger(PerformerStatusChecker.class);

	private HttpExchange exchange;

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE = "Content-Type";

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public PerformerStatusChecker(HttpExchange exchange) {
		this.exchange = exchange;
	}

	public void runStatusCheck() {
		logger.debug(" - Status check---> " + exchange.getRequestURI().toString());

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

			PerformerStatusCheckTuple statusCheckResult = new PerformerStatusCheckTuple();
			statusCheckResult.setPayload("A OK");
			statusCheckResult.setPerformerStatus(PerformerStatusType.PROCESSING);
			// convert the status check result to a string
			String returnValue = null;
			String payloadStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusCheckResult);

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
