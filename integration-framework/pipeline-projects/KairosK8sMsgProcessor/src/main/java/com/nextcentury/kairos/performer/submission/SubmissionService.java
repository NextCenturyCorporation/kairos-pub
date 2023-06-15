package com.nextcentury.kairos.performer.submission;

import com.nextcentury.kairos.ProcessorConfig;
import com.nextcentury.kairos.result.enqueue.MsgEnqueuer;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.nextcentury.kairos.performer.hrf.HRFSubmissionMessageHandler;
import com.nextcentury.kairos.performer.hrf.AHandler;

public class SubmissionService {
	private static final Logger logger = LogManager.getLogger(SubmissionService.class);

	public static final int serverPort = 10007;

	private static final String KAIROS_SERVICE = "/kairos";
	private static final String KAIROS_SUBMISSION_ENTRYPOINT = KAIROS_SERVICE + "/submission";
	
	private static final String KAIROS_HRF_SUBMISSION_ENTRYPOINT = KAIROS_SERVICE + "/hrf";

	public static void init(ProcessorConfig processorConfig) throws IOException {
		int cores = Runtime.getRuntime().availableProcessors();
		logger.info("# of cores on host - " + cores);

		// start l
		HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

		MsgEnqueuer msgEnqueuer = new MsgEnqueuer();

		logger.info("Creating context - " + KAIROS_SUBMISSION_ENTRYPOINT);
		server.createContext(KAIROS_SUBMISSION_ENTRYPOINT, (exchange -> {
			try {
				new SubmissionMessageHandler(msgEnqueuer, processorConfig).handleSubmissionMessage(exchange);
			} finally {
				exchange.close();
			}
		}));

		logger.info("Creating context - " + KAIROS_HRF_SUBMISSION_ENTRYPOINT);
		server.createContext(KAIROS_HRF_SUBMISSION_ENTRYPOINT, (exchange -> {
			try {
				new HRFSubmissionMessageHandler(msgEnqueuer, processorConfig).handle(exchange);
			} finally {
				exchange.close();
			}
		}));

		// server.setExecutor(Executors.newWorkStealingPool(cores));
		server.setExecutor(Executors.newFixedThreadPool(5));
		server.start();

		logger.info("");
		logger.info("Kairos Submission & HRF services ready ......");
		logger.info("");
	}

}
