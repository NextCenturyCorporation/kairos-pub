package com.nextcentury.kairos.performer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import com.nextcentury.kairos.performer.algorithm.entrypoint.io.EntrypointMessage;
import com.nextcentury.kairos.performer.algorithm.status.AlgorithmStatusChecker;
import com.nextcentury.kairos.performer.executor.AlgorithmExecutor;
import com.nextcentury.kairos.performer.healthcheck.PodStatusChecker;
import com.nextcentury.kairos.performer.submission.SubmissionMessage;
import com.nextcentury.kairos.restclient.RestClient;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class RestEntryPoint {
	private static Logger logger = null;
	private static final String APPLICATION_JSON = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";

	private static final String HTTP = "http://";

	private static final String KAIROS_SERVICE = "/kairos";
	private static final String KAIROS_SERVICE_ENTRYPOINT = KAIROS_SERVICE + "/entrypoint";
	private static final String KAIROS_SERVICE_STATUS = KAIROS_SERVICE + "/status";

	private static final String KAIROS_SERVICE_READY = KAIROS_SERVICE + "/ready";
	private static final String KAIROS_SERVICE_ALIVE = KAIROS_SERVICE + "/alive";

	private static final int serverPort = 10100;

	private static String evaluatorName;
	private static String experimentName;
	private static String performerName;
	private static String ta1SchemaLibPath;
	private static String graphgPath;

	private static String EXPERIMENT_CONFIG_KEY_EVALUATOR = "EVALUATOR";
	private static String EXPERIMENT_CONFIG_KEY_EXPERIMENT = "EXPERIMENT";
	private static String EXPERIMENT_CONFIG_KEY_PERFORMER = "PERFORMER_NAME";
	private static String EXPERIMENT_CONFIG_KEY_TA1SCHEMALIBPATH = "TA1SCHEMALIBPATH";
	private static String EXPERIMENT_CONFIG_KEY_GRAPHGPATH = "GRAPHGPATH";

	private static final String KAIROSFSMOUNTPATH_ROOT = "/var/kairosfs";

	private static String mountPathPersist;
	private static String mountPathLog;

	private static String enclave;
	private static String submissionServiceUrl;
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private static List<String> envErrorList = null;

	public static void main(String[] args) throws IOException {
		envErrorList = new ArrayList<String>();
		
		experimentName = validateEnvConfig(EXPERIMENT_CONFIG_KEY_EXPERIMENT);
		evaluatorName = validateEnvConfig(EXPERIMENT_CONFIG_KEY_EVALUATOR);
		performerName = validateEnvConfig(EXPERIMENT_CONFIG_KEY_PERFORMER);
		ta1SchemaLibPath = System.getenv().get(EXPERIMENT_CONFIG_KEY_TA1SCHEMALIBPATH);
		graphgPath = System.getenv().get(EXPERIMENT_CONFIG_KEY_GRAPHGPATH);

		if (envErrorList.size() != 0) {
			System.err.println("Following env config errors were found !!! ");
			System.err.println("");
			envErrorList.forEach(System.err::println);
			return;
		}

		mountPathPersist = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/persist").toString().toLowerCase();
		mountPathLog = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/log").toString().toLowerCase();

		// always do this first, so that log4j knows where to write the log file to
		configureLogging();

		// nist-expk-performera-msgprocessor.nist-expk-enclave.svc.cluster.local/kairos/submission
		enclave = new StringBuffer(evaluatorName).append("-").append(experimentName).append("-enclave").toString()
				.toLowerCase();
		submissionServiceUrl = new StringBuffer(HTTP).append(evaluatorName).append("-").append(experimentName)
				.append("-").append(performerName).append("-msgprocessor").append(".").append(enclave)
				.append(".svc.cluster.local/kairos/submission").toString().toLowerCase();

		// initialize and listen
		new RestEntryPoint().delegate();
	}

	private static void configureLogging() {
		System.setProperty("logfilelocation", mountPathLog);

		final LoggerContext ctx = (LoggerContext) LogManager.getContext();
		ctx.reconfigure();
		logger = LogManager.getLogger(RestEntryPoint.class);
		final Configuration config = ctx.getConfiguration();
		FileAppender fileAppender = config.getAppender("FILE");
		logger.debug("Log file name - " + fileAppender.getFileName());
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
			OutputStream responseBody = null;
			try {
				if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
					handleEntrypointMessage(exchange);
				} else {
					exchange.sendResponseHeaders(HttpStatus.SC_METHOD_NOT_ALLOWED, 0);
					responseBody = exchange.getResponseBody();
					responseBody.write("Only POST supported".getBytes());
				}
			} finally {
				if (responseBody != null) {
					try {
						responseBody.close();
					} catch (Throwable e) {
						logger.error(ExceptionHelper.getExceptionTrace(e));
					}
				}
				exchange.close();
			}
		}));

		logger.debug(" - Creating context - " + KAIROS_SERVICE_READY);
		server.createContext(KAIROS_SERVICE_READY, (exchange -> {
			try {
				new PodStatusChecker(exchange, PodStatusChecker.StatusType.READINESS_CHECK).runStatusCheck();
			} finally {
				exchange.close();
			}
		}));

		logger.debug(" - Creating context - " + KAIROS_SERVICE_ALIVE);
		server.createContext(KAIROS_SERVICE_ALIVE, (exchange -> {
			try {
				new PodStatusChecker(exchange, PodStatusChecker.StatusType.ALIVE_CHECK).runStatusCheck();
			} finally {
				exchange.close();
			}
		}));

		logger.debug(" - Creating context - " + KAIROS_SERVICE_STATUS);
		server.createContext(KAIROS_SERVICE_STATUS, (exchange -> {
			OutputStream responseBody = null;
			try {
				if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
					new AlgorithmStatusChecker(exchange).runStatusCheck();
				} else {
					exchange.sendResponseHeaders(HttpStatus.SC_OK, 0);
					responseBody = exchange.getResponseBody();
					responseBody.write("Only GET supported".getBytes());
				}
			} finally {
				if (responseBody != null) {
					try {
						responseBody.close();
					} catch (Throwable e) {
						logger.error(ExceptionHelper.getExceptionTrace(e));
					}
				}
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

	public void handleEntrypointMessage(HttpExchange exchange) {
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
			EntrypointMessage inputObject = mapper.readValue(buff.toString(), new TypeReference<EntrypointMessage>() {
			});

			// an actual AlgorithmExecutor might take the payload as a parameter
			// this is just to demonstrate the concept
			AlgorithmExecutor executor = new AlgorithmExecutor(performerName, inputObject);
			executor.execute();

			SubmissionMessage submission = new SubmissionMessage();
			submission.setPerformername(performerName);
			submission.setData(executor.getOutput());

			// convert submissionMessage to json string
			String payloadStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(submission);

			// call the msg processor with the submission
			callSubmissionService(payloadStr);

			exchange.sendResponseHeaders(executor.getStatusCode(), 0);
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

	private static String validateEnvConfig(String envKey) {
		Map<String, String> envMap = System.getenv();

		String value = null;
		if (!envMap.containsKey(envKey)) {
			envErrorList.add("Environment variable - " + envKey + " not provided.");
		} else {
			value = System.getenv().get(envKey);
			if (value == null || value.isEmpty()) {
				envErrorList.add("Environment variable - " + envKey + " has null or empty value.");
			} else {
			}
		}

		return value;
	}

	private String callSubmissionService(String output) {
		// invoke the submission service
		logger.debug("\tInvoking - " + submissionServiceUrl);
		String processingState = new RestClient(submissionServiceUrl, output).getResponse();
		logger.debug("\tReturned from rest call - " + processingState);

		return processingState;
	}
}
