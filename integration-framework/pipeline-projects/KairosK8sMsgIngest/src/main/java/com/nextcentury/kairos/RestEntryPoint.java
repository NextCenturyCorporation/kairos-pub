package com.nextcentury.kairos;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nextcentury.kairos.msgingest.listener.InputKafkaTopicListener;
import com.nextcentury.kairos.statuschecker.StatusChecker;
import com.nextcentury.kairos.utils.ExceptionHelper;
import com.sun.net.httpserver.HttpServer;

public class RestEntryPoint {
	//private static final Logger logger = LogManager.getLogger(RestEntryPoint.class);

	private static final String KAIROS_SERVICE_READY = "/ready";
	private static final String KAIROS_SERVICE_ALIVE = "/alive";

	private static final int serverPort = 10007;

	private static String evaluatorName;
	private static String experimentName;
	private static String inputRedisTopic;

	private static String kafkaBrokers;
	private static String inputKafkaTopic;
	private static String outputKafkaTopic;
	private static String errorKafkaTopic;

	private static boolean kairosPerformer;
	private static boolean aidaPerformer;

	private static String EXPERIMENT_CONFIG_KEY_EVALUATOR = "EVALUATOR";
	private static String EXPERIMENT_CONFIG_KEY_EXPERIMENT = "EXPERIMENT";
	private static String EXPERIMENT_CONFIG_KEY_KAFKA_BROKERS = "KAFKABROKERS";
	private static String EXPERIMENT_CONFIG_KEY_KAIROSLIB = "KAIROS_LIB";

	private static final String EXPERIMENT_CONFIG_KEY_KAIROSPERFORMER = "KAIROSPERFORMER";
	private static final String EXPERIMENT_CONFIG_KEY_AIDAPERFORMER = "AIDAPERFORMER";

	private InputKafkaTopicListener inputKafkaTopicListener = null;

	private static final int THREAD_POOL_SIZE = 10;
	private static ExecutorService executorService = null;

	private static String mountPathRoot;
	private static String mountPathInput;
	private static String mountPathOutput;
	private static String mountPathError;

	private static HttpServer server = null;

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler(Thread.currentThread())));

		evaluatorName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EVALUATOR);
		experimentName = System.getenv().get(EXPERIMENT_CONFIG_KEY_EXPERIMENT);
		kafkaBrokers = System.getenv().get(EXPERIMENT_CONFIG_KEY_KAFKA_BROKERS);
		kairosPerformer = Boolean.valueOf(System.getenv().get(EXPERIMENT_CONFIG_KEY_KAIROSPERFORMER));
		aidaPerformer = Boolean.valueOf(System.getenv().get(EXPERIMENT_CONFIG_KEY_AIDAPERFORMER));
		mountPathRoot = System.getenv().get(EXPERIMENT_CONFIG_KEY_KAIROSLIB);

		inputRedisTopic = new StringBuffer(evaluatorName).append("-").append(experimentName).append("-")
				.append("input-redis-topic").toString();

		inputKafkaTopic = new StringBuffer(evaluatorName).append("-").append(experimentName).append("-input-topic")
				.toString();
		outputKafkaTopic = new StringBuffer(evaluatorName).append("-").append(experimentName).append("-output-topic")
				.toString();
		errorKafkaTopic = new StringBuffer(evaluatorName).append("-").append(experimentName).append("-error-topic")
				.toString();

		initThreadPool();

		// if we have an aida performer
		if (aidaPerformer) {
			// defaults
			mountPathInput = new StringBuffer(mountPathRoot).append("/").append(experimentName)
					.append("/input").toString().toLowerCase();

			try {
				Path path = Paths.get(mountPathInput);

				// check to see if this folder exists
				if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
					// delete any input files
					System.out.println("Deleting pre-existing files in experiment input folder - " + mountPathInput);
					Files.walk(path).map(Path::toFile).forEach(File::delete);
					// delete folder
					System.out.println("Deleting experiment input folder - " + mountPathInput);
					Files.deleteIfExists(path);
				}

				System.out.println("Created experiment input folder - " + Files.createDirectories(path).toFile().getAbsolutePath());

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		new RestEntryPoint().delegateTopicListener();
	}

	private static void initThreadPool() {
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("# of cores on host - " + cores);

		executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		executorService.submit(() -> System.out.println("Thread pool started"));
	}

	public RestEntryPoint() {
		// set up the readiness check server
		try {
			server = HttpServer.create(new InetSocketAddress(serverPort), 0);
			// ready checkl
			System.out.println("Creating readiness check context - " + KAIROS_SERVICE_READY);
			server.createContext(KAIROS_SERVICE_READY, (exchange -> {
				try {
					new StatusChecker(exchange, StatusChecker.StatusType.READINESS_CHECK).runStatusCheck();
				} finally {
					exchange.close();
				}
			}));
			// liveness checkl
			System.out.println("Creating liveness check context - " + KAIROS_SERVICE_ALIVE);
			server.createContext(KAIROS_SERVICE_ALIVE, (exchange -> {
				try {
					new StatusChecker(exchange, StatusChecker.StatusType.READINESS_CHECK).runStatusCheck();
				} finally {
					exchange.close();
				}
			}));

			// server.setExecutor(Executors.newWorkStealingPool(cores));
			server.setExecutor(Executors.newFixedThreadPool(5));
			server.start();
		} catch (IOException e) {
			System.err.println(ExceptionHelper.getExceptionTrace(e));
		}
	}

	private void delegateTopicListener() {
		KafkaConfig kafkaConfig = new KafkaConfig(kafkaBrokers, inputKafkaTopic, outputKafkaTopic, errorKafkaTopic);
		ServiceInvocationConfig svcConfig = new ServiceInvocationConfig(aidaPerformer, kairosPerformer, mountPathInput,
				inputRedisTopic);
		inputKafkaTopicListener = new InputKafkaTopicListener(evaluatorName, experimentName, kafkaConfig,
				executorService, svcConfig);
		inputKafkaTopicListener.runConsumer();
	}

	private static class ShutdownHandler implements Runnable {
		private static final int AWAIT_TERMINATION_TIMEOUT_SECS = 10;
		private Thread mainThread;

		public ShutdownHandler(Thread mainThread) {
			this.mainThread = mainThread;
		}

		@Override
		public void run() {
			System.out.println("Inside shutdown handler..");

			try {
				if (!executorService.isShutdown() || !executorService.isTerminated()) {
					System.out.println("Attempting graceful shutdown threadpool....");
					executorService.shutdown();

					System.out.println(
							"Looks like threads in the pool are still working, waiting 10 seconds before enforcing shutdown..");
					if (!executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT_SECS, TimeUnit.SECONDS)) {
						System.out.println(
								"Timeout (" + AWAIT_TERMINATION_TIMEOUT_SECS + " seconds) elapsed, before termination");
					}

					if (!executorService.isShutdown() || !executorService.isTerminated()) {
						System.out.println("Threadpool still not shutdown, invoking abrupt shutdown");

						System.out.println("Shutting down threadpool abruptly....");
						executorService.shutdownNow();
					}
				}
			} catch (Throwable e) {
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			} finally {
				// give control back to the main thread
				try {
					mainThread.join();
				} catch (Throwable e1) {
					System.err.println(ExceptionHelper.getExceptionTrace(e1));
				}
			}
		}
	}
}
