package com.nextcentury.kairos.performer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import com.nextcentury.kairos.performer.executor.AlgorithmExecutor;
import com.nextcentury.kairos.performer.healthcheck.StatusChecker;
import com.nextcentury.kairos.tuple.KairosMessage;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.nextcentury.kairos.utils.StatusCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Main entry class
 * 
 * - sets up the container environment - starts up thread pooling - starts up
 * file system folder monitoring
 * 
 * @author kdeshpande
 *
 */
public class RestEntryPoint {
	private static final Logger logger = LogManager.getLogger(RestEntryPoint.class);
	private static final String APPLICATION_JSON = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";

	private static final String KAIROS_SERVICE = "/kairos";
	private static final String KAIROS_SERVICE_ENTRYPOINT = KAIROS_SERVICE + "/entrypoint";

	private static final String KAIROS_SERVICE_READY = KAIROS_SERVICE + "/ready";
	private static final String KAIROS_SERVICE_ALIVE = KAIROS_SERVICE + "/alive";

	private static final int serverPort = 10100;

	private static String evaluatorName;
	private static String experimentName;
	private static String performerName;

	private static String EXPERIMENT_CONFIG_KEY_EVALUATOR = "EVALUATOR";
	private static String EXPERIMENT_CONFIG_KEY_EXPERIMENT = "EXPERIMENT";
	private static String EXPERIMENT_CONFIG_KEY_PERFORMER = "PERFORMER_NAME";

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public static void main(String[] args) throws IOException {
		experimentName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EXPERIMENT);
		evaluatorName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EVALUATOR);
		performerName = System.getenv().get(EXPERIMENT_CONFIG_KEY_PERFORMER);

		// initialize and listen
		new RestEntryPoint().delegate();
	}

	private void delegate() throws IOException {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug("# of cores on host - " + cores);

		logger.debug("Experiment Name - " + experimentName);
		logger.debug("Evaluator Name - " + evaluatorName);
		logger.debug("Performer Name - " + performerName);

		// start l
		HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
		logger.debug("Creating context - " + KAIROS_SERVICE_ENTRYPOINT);
		server.createContext(KAIROS_SERVICE_ENTRYPOINT, (exchange -> {
			try {
				handleMessage(exchange);
			} finally {
				exchange.close();
			}
		}));

		logger.debug(" - Creating context - " + KAIROS_SERVICE_READY);
		server.createContext(KAIROS_SERVICE_READY, (exchange -> {
			try {
				new StatusChecker(exchange, StatusChecker.StatusType.READINESS_CHECK).runStatusCheck();
			} finally {
				exchange.close();
			}
		}));

		logger.debug(" - Creating context - " + KAIROS_SERVICE_ALIVE);
		server.createContext(KAIROS_SERVICE_ALIVE, (exchange -> {
			try {
				new StatusChecker(exchange, StatusChecker.StatusType.ALIVE_CHECK).runStatusCheck();
			} finally {
				exchange.close();
			}
		}));

		// server.setExecutor(Executors.newWorkStealingPool(cores));
		server.setExecutor(Executors.newFixedThreadPool(5));
		server.start();

		logger.debug("");
		logger.debug("Kairos Test Performer service ready ......");
		logger.debug("");
	}

	public void handleMessage(HttpExchange exchange) {
		logger.debug("URI ---> " + exchange.getRequestURI().toString());

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
			// payload is what was sent in with the REST call
			String payload = buff.toString();

			KairosMessage inputObject = mapper.readValue(payload, new TypeReference<KairosMessage>() {
			});
			
			// an actual AlgorithmExecutor might take the payload as a parameter
			// this is just to demonstrate the concept
			String returnValue = new AlgorithmExecutor(inputObject).execute();

			exchange.sendResponseHeaders(StatusCode.OK.getCode(), 0);
			// write the response back
			exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
			responseBody = exchange.getResponseBody();
			responseBody.write(returnValue.getBytes());
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

	public void handleErrorMessage(HttpExchange exchange) {
		logger.debug("URI ---> " + exchange.getRequestURI().toString());

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
			String payload = buff.toString();

			// set the processed result
			String returnValue = "Sending back an error  - " + payload;

			exchange.sendResponseHeaders(StatusCode.BAD_REQUEST.getCode(), 0);
			// write the response back
			exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
			responseBody = exchange.getResponseBody();
			responseBody.write(returnValue.getBytes());
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
