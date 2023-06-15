package com.nextcentury.kairos;

import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;

import com.nextcentury.kairos.utils.ExceptionHelper;

public class RedisTopicSubscriber {
	//private static final Logger logger = LogManager.getLogger(RedisTopicSubscriber.class);

	private static final String REDIS_SERVICE = "redis://redis-cluster.kairos-redis:6379";

	private static RedissonClient redisson = null;

	private static final Logger logger = LogManager.getLogger(RedisTopicSubscriber.class);

	// this will be pub -sub later
	// private static RQueue<String> inputQueue = null;

	private RTopic inputRedisTopic = null;
	private ExecutorService executorService;

	static {
		if (redisson == null) {
			try {
				Config config = new Config();
				config.useClusterServers().addNodeAddress(REDIS_SERVICE);
				redisson = Redisson.create(config);
			} catch (Throwable e) {
				// e.printStackTrace();
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			}
		}
		// inputQueue = redisson.getQueue("MsgInputQueue");
	}

	private ProcessorConfig processorConfig;
	private KairosMsgProcessor msgProcessor;

	public RedisTopicSubscriber(ProcessorConfig processorConfig, ExecutorService executorService) {
		super();
		this.processorConfig = processorConfig;
		this.executorService = executorService;
		inputRedisTopic = redisson.getTopic(this.processorConfig.getInputRedisTopic());
		msgProcessor = new KairosMsgProcessor(this.processorConfig, this.executorService);
	}

	public void init() {
		inputRedisTopic.addListener(String.class, new MessageListener<String>() {
			@Override
			public void onMessage(CharSequence charSequence, String message) {
				logger.info("Msg arrived at topic - " + processorConfig.getInputRedisTopic() + " with content : "
						+ message);
				msgProcessor.processMessage(message);
			}
		});
	}
}
