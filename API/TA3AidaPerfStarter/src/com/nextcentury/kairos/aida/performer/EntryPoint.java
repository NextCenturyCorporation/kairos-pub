package com.nextcentury.kairos.aida.performer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.nextcentury.kairos.aida.performer.algorithm.status.AlgorithmStatusChecker;
import com.nextcentury.kairos.aida.performer.healthcheck.PodStatusChecker;
import com.sun.net.httpserver.HttpServer;

public class EntryPoint {
	private static Logger logger = null;

	private static String evaluatorName;
	private static String experimentName;
	private static String performerName;
	private static String ta1SchemaLibPath;
	private static String graphgPath;

	private static String mountPathPersist;
	private static String mountPathLog;

	private static final int serverPort = 10100;

	private static String EXPERIMENT_CONFIG_KEY_EVALUATOR = "EVALUATOR";
	private static String EXPERIMENT_CONFIG_KEY_EXPERIMENT = "EXPERIMENT";
	private static String EXPERIMENT_CONFIG_KEY_PERFORMER = "PERFORMER_NAME";
	private static String EXPERIMENT_CONFIG_KEY_TA1SCHEMALIBPATH = "TA1SCHEMALIBPATH";
	private static String EXPERIMENT_CONFIG_KEY_GRAPHGPATH = "GRAPHGPATH";

	private static final String KAIROS_SERVICE = "/kairos";
	private static final String KAIROS_SERVICE_STATUS = KAIROS_SERVICE + "/status";

	private static final String KAIROS_SERVICE_READY = KAIROS_SERVICE + "/ready";
	private static final String KAIROS_SERVICE_ALIVE = KAIROS_SERVICE + "/alive";

	private static final String KAIROSFSMOUNTPATH_ROOT = "/var/kairosfs";

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private static final int THREAD_POOL_SIZE = 10;
	private static ExecutorService executorService = null;

	public static void main(String[] args) throws IOException {
		experimentName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EXPERIMENT);
		evaluatorName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EVALUATOR);
		performerName = System.getenv().get(EXPERIMENT_CONFIG_KEY_PERFORMER);
		ta1SchemaLibPath = System.getenv().get(EXPERIMENT_CONFIG_KEY_TA1SCHEMALIBPATH);
		graphgPath = System.getenv().get(EXPERIMENT_CONFIG_KEY_GRAPHGPATH);

		mountPathPersist = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/persist").toString().toLowerCase();
		mountPathLog = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/log").toString().toLowerCase();

		// always do this first, so that log4j knows where to write the log file to
		configureLogging();

		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug("# of cores on host - " + cores);

		HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

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
					exchange.sendResponseHeaders(HttpStatus.SC_METHOD_NOT_ALLOWED, 0);
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
		logger.debug(" Finished setting up http server ");

		initThreadPool();

		// initialize and listen
		new EntryPoint().delegate();
	}

	private static void configureLogging() {
		System.setProperty("logfilelocation", mountPathLog);

		final LoggerContext ctx = (LoggerContext) LogManager.getContext();
		ctx.reconfigure();
		logger = LogManager.getLogger(EntryPoint.class);
		final Configuration config = ctx.getConfiguration();
		FileAppender fileAppender = config.getAppender("FILE");
		logger.debug("Log file name - " + fileAppender.getFileName());
	}

	private static void initThreadPool() {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug(" - # of cores on host - " + cores);

		executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		executorService.submit(() -> logger.debug("Thread pool started"));
	}

	private void delegate() throws IOException {
		// has any path been passed in?
		String mountPathIn = null;
		String mountPathOut = null;
		String mountPathError = null;
		String mountPathPersist = null;
		String mountPathLog = null;

		mountPathOut = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/output").toString().toLowerCase();
		mountPathError = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/error").toString().toLowerCase();
		mountPathPersist = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/persist").toString().toLowerCase();
		mountPathLog = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
				.append(performerName).append("/log").toString().toLowerCase();
		mountPathIn = new StringBuffer(KAIROSFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/input")
				.toString().toLowerCase();

		// monitor input folder, process and write to output folder
		new InputPathMonitor(performerName, mountPathIn, mountPathOut, mountPathError, mountPathLog, executorService).start();

		logger.debug("");
		logger.debug("Kairos Aida Test Performer service ready ......");
		logger.debug("");
	}
}
