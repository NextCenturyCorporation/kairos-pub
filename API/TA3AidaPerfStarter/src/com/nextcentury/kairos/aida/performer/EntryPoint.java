package com.nextcentury.kairos.aida.performer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Main entry class
 * 
 * - sets up the container environment - starts up thread pooling - starts up
 * file system folder monitoring
 * 
 * @author kdeshpande
 *
 */
public class EntryPoint {
	private static final Logger logger = LogManager.getLogger(EntryPoint.class);

	private static String evaluatorName;
	private static String experimentName;
	private static String performerName;

	private static String EXPERIMENT_CONFIG_KEY_EVALUATOR = "EVALUATOR";
	private static String EXPERIMENT_CONFIG_KEY_EXPERIMENT = "EXPERIMENT";
	private static String EXPERIMENT_CONFIG_KEY_PERFORMER = "PERFORMER_NAME";

	private static final String EXPERIMENT_CONFIG_KEY_AIDAINPUTPATH = "AIDAINPUTPATH";
	private static final String EXPERIMENT_CONFIG_KEY_AIDAOUTPUTPATH = "AIDAOUTPUTPATH";
	private static final String EXPERIMENT_CONFIG_KEY_AIDAERRORPATH = "AIDAERRORPATH";

	private static final String AIDAFSMOUNTPATH_ROOT = "/var/aidafs";

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private static final int THREAD_POOL_SIZE = 10;
	private static ExecutorService executorService = null;

	public static void main(String[] args) throws IOException {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug("# of cores on host - " + cores);

		experimentName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EXPERIMENT);
		evaluatorName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EVALUATOR);
		performerName = System.getenv().get(EXPERIMENT_CONFIG_KEY_PERFORMER);

		initThreadPool();

		// initialize and listen
		new EntryPoint().delegate();
	}

	private static void initThreadPool() {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.debug(" - # of cores on host - " + cores);

		executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		executorService.submit(() -> logger.debug("Thread pool started"));
	}

	private void delegate() throws IOException {
		// has any path been passed in?
		String mountPathIn = System.getenv().get(EXPERIMENT_CONFIG_KEY_AIDAINPUTPATH);
		String mountPathOut = System.getenv().get(EXPERIMENT_CONFIG_KEY_AIDAOUTPUTPATH);
		String mountPathError = System.getenv().get(EXPERIMENT_CONFIG_KEY_AIDAERRORPATH);

		if (mountPathOut == null || mountPathOut.isEmpty() || mountPathOut.equalsIgnoreCase("{{aidaoutputpath}}")) {
			// defaults
			mountPathOut = new StringBuffer(AIDAFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
					.append(performerName).append("/output").toString().toLowerCase();
		}

		if (mountPathError == null || mountPathError.isEmpty()
				|| mountPathError.equalsIgnoreCase("{{aidaerrorpath}}")) {
			// defaults
			mountPathError = new StringBuffer(AIDAFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/")
					.append(performerName).append("/error").toString().toLowerCase();
		}

		if (mountPathIn == null || mountPathIn.isEmpty() || mountPathIn.equalsIgnoreCase("{{aidainputpath}}")) {
			// defaults
			mountPathIn = new StringBuffer(AIDAFSMOUNTPATH_ROOT).append("/").append(experimentName).append("/input")
					.toString().toLowerCase();
		}

		// monitor input folder, process and write to output folder
		new InputPathMonitor(performerName, mountPathIn, mountPathOut, executorService).start();

		logger.debug("");
		logger.debug("Kairos Aida Test Performer service ready ......");
		logger.debug("");
	}
}
