package com.nextcentury.kairos.aida.performer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import com.nextcentury.kairos.aida.performer.tuple.KairosMessage;
import com.nextcentury.kairos.aida.performer.tuple.ReturnValueTuple;

public class InputPathMonitor {
	private static final Logger logger = LogManager.getLogger(InputPathMonitor.class);
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private String performerName;
	private String inputPathStr;
	private String outputPathStr;
	private String logPathStr;
	private ExecutorService executorService;

	public InputPathMonitor(String performerName, String inputPathStr, String outputPathStr, String logPathStr,
			ExecutorService executorService) {
		this.inputPathStr = inputPathStr;
		this.outputPathStr = outputPathStr;
		this.logPathStr = logPathStr;
		this.performerName = performerName;
		this.executorService = executorService;
	}

	public void start() {
		FileAlterationObserver observer = new FileAlterationObserver(inputPathStr);

		logger.debug("Start Monitoring - " + inputPathStr);
		observer.addListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileCreate(File file) {
				logger.debug("File Created:" + file.getName());
				executorService.submit(new WorkerThread(file, outputPathStr));
			}

			@Override
			public void onFileDelete(File file) {
				logger.debug("File Deleted:" + file.getName());
			}
		});

		/* Set to monitor changes for 500 ms */
		FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);
		try {
			monitor.start();
		} catch (Exception e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		}
	}

	class WorkerThread implements Runnable {
		private File file;
		private String outputPathStr;

		public WorkerThread(File file, String outputPathStr) {
			super();
			this.file = file;
			this.outputPathStr = outputPathStr;
		}

		@Override
		public void run() {
			KairosMessage inputObject = null;
			try {
				String fileName = file.getAbsoluteFile().toString();
				logger.debug("Reading input file - " + fileName);
				// this is the content of the result file
				String payload = new String(Files.readAllBytes(Paths.get(fileName)));
				inputObject = mapper.readValue(payload, new TypeReference<KairosMessage>() {
				});

				// invoke the algorithm
				// an real performer will pass in the payload
				String result = new AlgorithmExecutor(inputObject).execute();

				ReturnValueTuple valueTuple = new ReturnValueTuple();
				valueTuple.setStatusCode(HttpStatus.SC_OK);
				valueTuple.setValue(result);

				// sign the output with performer name
				inputObject.setProcessResult(valueTuple);

				// convert it back to a string
				String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputObject);
				// logger.debug(content);

				// send the message on its way, by writing to output path
				String outputFileName = new StringBuffer(outputPathStr).append("/").append(Instant.now().toString())
						.toString();
				Files.write(Paths.get(outputFileName), content.getBytes(), StandardOpenOption.CREATE);

				logger.debug("----Wrote file " + outputFileName);
				// logger.debug();
				// logger.debug(content);
				logger.debug("");
				logger.debug("");
			} catch (Exception e) {
				logger.error(ExceptionHelper.getExceptionTrace(e));
			}
		}
	}

	private static void listFilesInit(String inputPathStr) {
		logger.debug("Current contents - ");
		try (Stream<Path> walk = Files.walk(Paths.get(inputPathStr))) {
			List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).sorted()
					.collect(Collectors.toList());
			result.forEach(file -> {
				logger.debug("\t" + file);
			});
		} catch (IOException e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		}
	}
}
