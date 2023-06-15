package com.nextcentury.kairos;

import com.nextcentury.kairos.performer.algorithm.entrypoint.io.EntrypointMessage;
import com.nextcentury.kairos.performer.algorithm.entrypoint.io.EntrypointResponse;
import com.nextcentury.kairos.restclient.RestClient;
import com.nextcentury.kairos.utils.ExceptionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.Date;
import java.util.concurrent.ExecutorService;

public class KairosMsgProcessor {

	private static final String DEFAULT_PORT = "80";

	private static final Logger logger = LogManager.getLogger(KairosMsgProcessor.class);

	private static final String DOT = ".";

	private static final String STARTING_BACKSLASH = "/";

	private static final String ENCLAVE = "enclave";

	private static final String DASH = "-";

	private static final String HTTP_PREFIX = "http://";

	private ExecutorService executorService;

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private String evaluatorName;
	private String experimentName;
	private String performerName;
	private String serviceName;
	private String port;
	private String entryPointpathSpec;

	public KairosMsgProcessor(ProcessorConfig processorConfig, ExecutorService executorService) {
		evaluatorName = processorConfig.getEvaluatorName();
		experimentName = processorConfig.getExperimentName();
		performerName = processorConfig.getPerformerName();
		serviceName = processorConfig.getServiceName();
		port = processorConfig.getPort();
		if (port == null || port.isEmpty()) {
			port = DEFAULT_PORT;
		}
		entryPointpathSpec = processorConfig.getEntryPointpathSpec();

		this.executorService = executorService;
	}

	public void processMessage(String msg) {
		executorService.submit(new WorkerThread(msg));
	}

	class WorkerThread implements Runnable {
		private static final String SVC_CLUSTER_LOCAL = ".svc.cluster.local";
		private final String msg;
		private int statusCode;
		private EntrypointResponse outputObject;

		public WorkerThread(String msg) {
			this.msg = msg;
		}

		public void run() {
			processMessage(msg);
		}

		private void processMessage(String msg) {
			logger.info("Begin processing by performer - " + performerName);
			Date start = new Date();

			// process message
			// the output is thrown away, since it is the result of the submission call back
			// that is sent along further in the pipeline
			outputObject = invokePerformerService(msg);

			Date end = new Date();
			Interval interval = new Interval(start.getTime(), end.getTime());
			Period period = interval.toPeriod();
			logger.info(String.format("MsgProcessor Time taken - %d hours, %d minutes, %d seconds, %d millis%n",
					period.getHours(), period.getMinutes(), period.getSeconds(), period.getMillis()));
			logger.info("End processing by performer - " + performerName);
		}

		private EntrypointResponse invokePerformerService(String msg) {
			EntrypointMessage inputObject = null;
			outputObject = new EntrypointResponse();
			try {
				inputObject = mapper.readValue(msg, new TypeReference<EntrypointMessage>() {
				});

				String enclaveName = new StringBuffer(evaluatorName).append(DASH).append(experimentName).append(DASH)
						.append(ENCLAVE).toString().toLowerCase();
				String entrypointpath = entryPointpathSpec;
				if (!entrypointpath.startsWith(STARTING_BACKSLASH)) {
					entrypointpath = STARTING_BACKSLASH.concat(entrypointpath);
				}

				// service.enclave.svc.cluster.local:port/path/path
				String fqPerformerServiceName = new StringBuffer(HTTP_PREFIX).append(serviceName).append(DOT)
						.append(enclaveName).append(SVC_CLUSTER_LOCAL).append(":").append(port).append(entrypointpath)
						.toString().toLowerCase();

				
				outputObject.setRequestId(inputObject.getId());
				// invoke the performer service
				logger.info("Invoking - " + fqPerformerServiceName);
				RestClient restClient = new RestClient(fqPerformerServiceName, msg);
				restClient.execute();
				// since this is a "fire and forget", we dont really care about output
				// nor do we wait for response
				outputObject.setContent(restClient.getResponse());
				statusCode = restClient.getStatusCode();
				logger.info("http status code - " + statusCode);
				logger.info("Content - " + outputObject.getContent());

			} catch (Throwable e) {
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			}

			return outputObject;
		}
	}
}
