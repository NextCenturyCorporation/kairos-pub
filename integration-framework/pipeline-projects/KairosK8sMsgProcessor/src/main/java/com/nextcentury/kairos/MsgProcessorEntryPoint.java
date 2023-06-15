package com.nextcentury.kairos;

import com.nextcentury.kairos.performer.submission.SubmissionService;
import com.nextcentury.kairos.utils.ExceptionHelper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MsgProcessorEntryPoint {
    
    private static final int THREAD_POOL_SIZE = 10;

    private static final Logger logger = LogManager.getLogger(MsgProcessorEntryPoint.class);

    private static final int serverPort = 10100;

    private static final String ENV_KEY_KAFKA_BROKERS = "KAFKABROKERS";
    private static final String ENV_KEY_PERFORMER_NAME = "PERFORMER_NAME";
    private static final String ENV_KEY_EXPERIMENT = "EXPERIMENT";
    private static final String ENV_KEY_EVALUATOR = "EVALUATOR";
    private static final String ENV_KEY_SERVICE_NAME = "SERVICE_NAME";
    private static final String ENV_KEY_START_TIME_ID = "START_TIME_ID";
    private static final String ENV_KEY_PORT = "PORT";
    private static final String ENV_KEY_ENTRYPOINTPATHSPEC = "ENTRYPOINTPATHSPEC";
    private static final String ENV_KEY_READINESSCHECKPATHSPEC = "READINESSCHECKPATHSPEC";
    private static final String ENV_KEY_LIVENESSCHECKPATHSPEC = "LIVENESSCHECKPATHSPEC";

    private static final String ENV_KEY_ESDOMAIN_ENDPOINT = "ESDOMAIN_ENDPOINT";

    private static String evaluatorName;
    private static String experimentName;
    private static String performerName;
    private static String kafkaBrokers;

    private static String serviceName;
    private static String startTimeId;
    private static String port;
    private static String entryPointpathSpec;
    private static String readinessCheckpathSpec;
    private static String livenessCheckpathSpec;

    private static String esHost;

    private static final String DOT = ".";

    private static final String STARTING_BACKSLASH = "/";

    private static final String ENCLAVE = "enclave";

    private static final String DASH = "-";

    private static final String HTTP_PREFIX = "http://";

    private static final String SVC_CLUSTER_LOCAL = ".svc.cluster.local";

    private static ExecutorService executorService;

    public MsgProcessorEntryPoint() {
        super();
    }

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler(Thread.currentThread())));

        kafkaBrokers = System.getenv().get(ENV_KEY_KAFKA_BROKERS);
        evaluatorName = System.getenv().get(ENV_KEY_EVALUATOR);
        experimentName = System.getenv().get(ENV_KEY_EXPERIMENT);
        performerName = System.getenv().get(ENV_KEY_PERFORMER_NAME);

        serviceName = System.getenv().get(ENV_KEY_SERVICE_NAME);
        startTimeId = System.getenv().get(ENV_KEY_START_TIME_ID);

        port = System.getenv().get(ENV_KEY_PORT);

        entryPointpathSpec = System.getenv().get(ENV_KEY_ENTRYPOINTPATHSPEC);
        readinessCheckpathSpec = System.getenv().get(ENV_KEY_READINESSCHECKPATHSPEC);
        livenessCheckpathSpec = System.getenv().get(ENV_KEY_LIVENESSCHECKPATHSPEC);

        esHost = System.getenv().get(ENV_KEY_ESDOMAIN_ENDPOINT);

        String fqPerformerServiceUrl = getFQPerformerService();

        ProcessorConfig processorConfig = new ProcessorConfig.Builder()
                .kafkaBrokers(kafkaBrokers)
                .evaluator(evaluatorName)
                .experiment(experimentName)
                .performer(performerName)
                .service(serviceName)
                .startTimeId(startTimeId)
                .port(port)
                .entryPointpathSpec(entryPointpathSpec)
                .fqPerformerSericeUrl(fqPerformerServiceUrl)
                .esHost(esHost)
                .build();

        // init Services
        SubmissionService.init(processorConfig);

        initThreadPool();
        // initialize and listen
        logger.info("Service listening for messages on topic - " + processorConfig.getInputRedisTopic()
                + " and publish results to " + processorConfig.getResultQName() + ", errors to "
                + processorConfig.getErrorQName());
        // initialize the subscriber client and start listening
        new RedisTopicSubscriber(processorConfig, executorService).init();
    }

    private static String getFQPerformerService() {
        String enclaveName = new StringBuffer(evaluatorName).append(DASH).append(experimentName).append(DASH)
                .append(ENCLAVE).toString().toLowerCase();
        if (!entryPointpathSpec.startsWith(STARTING_BACKSLASH)) {
            entryPointpathSpec = STARTING_BACKSLASH.concat(entryPointpathSpec);
        }

        // service.enclave.svc.cluster.local/kairos/status
        String fqPerformerServiceUrl = new StringBuffer(HTTP_PREFIX).append(serviceName).append(DOT).append(enclaveName)
                .append(SVC_CLUSTER_LOCAL).append(":").append(port).append(entryPointpathSpec).toString().toLowerCase();

        logger.info("");
        logger.info("Performer service - fq url - " + fqPerformerServiceUrl);
        logger.info("");
        return fqPerformerServiceUrl;
    }

    private static void initThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        logger.info(" - # of cores on host - " + cores);

        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        executorService.submit(() -> logger.info("Thread pool started"));
    }

    private static void dumpEnvConfig() {
        Map<String, String> envMap = System.getenv();
        envMap.entrySet().stream().forEach(entry -> {
            logger.info(entry.getKey() + " - " + entry.getValue());
        });
    }

    private static class ShutdownHandler implements Runnable {
        private static final int AWAIT_TERMINATION_TIMEOUT_SECS = 10;
        private Thread mainThread;

        public ShutdownHandler(Thread mainThread) {
            this.mainThread = mainThread;
        }

        @Override
        public void run() {
            logger.info("Inside shutdown handler..");

            try {
                if (!executorService.isShutdown() || !executorService.isTerminated()) {
                    logger.info("Attempting graceful shutdown threadpool....");
                    executorService.shutdown();

                    logger.info(
                            "Looks like threads in the pool are still working, waiting 10 seconds before enforcing shutdown..");
                    if (!executorService.awaitTermination(AWAIT_TERMINATION_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                        logger.info(
                                "Timeout (" + AWAIT_TERMINATION_TIMEOUT_SECS + " seconds) elapsed, before termination");
                    }

                    if (!executorService.isShutdown() || !executorService.isTerminated()) {
                        logger.info("Threadpool still not shutdown, invoking abrupt shutdown");

                        logger.info("Shutting down threadpool abruptly....");
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
