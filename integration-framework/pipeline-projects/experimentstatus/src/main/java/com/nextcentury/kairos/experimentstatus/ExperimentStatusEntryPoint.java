package com.nextcentury.kairos.experimentstatus;

import com.nextcentury.kairos.experimentstatus.tuple.ExperimentStatus;
import com.nextcentury.kairos.experimentstatus.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class ExperimentStatusEntryPoint {

	private static final Logger logger = LogManager.getLogger(ExperimentStatusEntryPoint.class);
	private static final String APPLICATION_JSON = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";

	private static final String KAIROS_EXPERIMENT_STATUS_SERVICE = "/kairos/experimentstatus";

	private static final int serverPort = 10120;

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public static void main(String[] args) throws IOException {
		// initialize and delegate
		new ExperimentStatusEntryPoint().delegate();
	}

	private void delegate() throws IOException {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug("# of cores on host - " + cores);

		// start l
		HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

		logger.debug("Creating context - " + KAIROS_EXPERIMENT_STATUS_SERVICE);
		server.createContext(KAIROS_EXPERIMENT_STATUS_SERVICE, (exchange -> {
			try {
				getExperimentContainerStatus(exchange);
			} finally {
				exchange.close();
			}
		}));

		// server.setExecutor(Executors.newWorkStealingPool(cores));
		server.setExecutor(Executors.newFixedThreadPool(5));
		server.start();

		logger.debug("");
		logger.debug("Kairos Experiment Status service ready ......");
		logger.debug("");
	}

	private void getExperimentContainerStatus(HttpExchange exchange) {
		String uriRequest = exchange.getRequestURI().getQuery();
		logger.debug("URI ---> " + exchange.getRequestURI().getPath());

		OutputStream responseBody = null;
		BufferedReader reader = null;
		InputStreamReader isReader = null;

		try {
			StringBuffer buff = new StringBuffer();
			String line;
			isReader = new InputStreamReader(exchange.getRequestBody());
			reader = new BufferedReader(isReader);
			while ((line = reader.readLine()) != null) {
				buff.append(line);
			}

			// get list of experiment namespaces within this cluster
			List<ExperimentStatus> experimentStatuses = new ExperimentStatusProvider().getAllExperimentsStatus();
			ExperimentStatusResponse response = new ExperimentStatusResponse();
			response.setStatusList(experimentStatuses);

			// convert to json
			String returnValue = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
			logger.debug(returnValue);

			exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
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
