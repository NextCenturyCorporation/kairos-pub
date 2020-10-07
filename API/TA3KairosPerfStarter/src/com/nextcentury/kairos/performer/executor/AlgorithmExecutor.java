package com.nextcentury.kairos.performer.executor;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.nextcentury.kairos.performer.algorithm.entrypoint.io.EntrypointMessage;
import com.nextcentury.kairos.utils.ExceptionHelper;

public class AlgorithmExecutor {
	private static final Logger logger = LogManager.getLogger(AlgorithmExecutor.class);

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private EntrypointMessage input;
	private String output;
	private String performerName;
	private int statusCode;

	public AlgorithmExecutor(String performerName, EntrypointMessage input) {
		super();
		this.input = input;
		this.performerName = performerName;
	}

	public String getOutput() {
		return output;
	}

	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * this method will return an actual json document
	 * 
	 * @return
	 */
	public void execute() {
		try {
			String payloadStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
			logger.debug("received - ");
			logger.debug(payloadStr);

			// invoke algorthm implementation here
			// return string value json returned from execution in the returnValue field

			// 
			// dummy return value
			//
			output = "Successfully processed from test performer container - " + performerName;
			
			//
			// simulating a successful return
			//
			statusCode = HttpStatus.SC_OK;
		} catch (Throwable e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		}
	}
}
