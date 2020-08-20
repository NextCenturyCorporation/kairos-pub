package com.nextcentury.kairos.performer.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.nextcentury.kairos.tuple.KairosMessage;
import com.nextcentury.kairos.utils.ExceptionHelper;

/**
 * Class for performers to plug-in their implementations
 * 
 * @author kdeshpande
 *
 */
public class AlgorithmExecutor {
	private static final Logger logger = LogManager.getLogger(AlgorithmExecutor.class);

	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	private KairosMessage input;

	public AlgorithmExecutor(KairosMessage input) {
		super();
		this.input = input;
	}

	/**
	 * this method will return an actual json document
	 * 
	 * @return
	 */
	public String execute() {
		String returnValue = null;
		try {
			String payloadStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
			logger.debug("received - ");
			logger.debug(payloadStr);

			// invoke algorthm implementation here
			// return value returned from execution in the returnValue field

			// dummy return value
			returnValue = "Successfully processed from kairos-style test performer container";
		} catch (Throwable e) {
			logger.error(ExceptionHelper.getExceptionTrace(e));
		}

		return returnValue;
	}
}
