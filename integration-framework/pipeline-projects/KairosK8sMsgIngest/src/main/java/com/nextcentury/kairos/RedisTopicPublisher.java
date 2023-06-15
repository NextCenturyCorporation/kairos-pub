package com.nextcentury.kairos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.nextcentury.kairos.utils.ExceptionHelper;

public class RedisTopicPublisher {
	//private static final Logger logger = LogManager.getLogger(RedisTopicPublisher.class);
	private static final String REDIS_SERVICE = "redis://redis-cluster.kairos-redis:6379";

	private static RedissonClient redisson = null;

	// this will be pub -sub later
	// private static RQueue<String> inputQueue = null;
	private String topicName = null;

	private RTopic publishTopic = null;
	static {
		if (redisson == null) {
			try {
				Config config = new Config();
				config.useClusterServers().addNodeAddress(REDIS_SERVICE);
				redisson = Redisson.create(config);
			} catch (Throwable e) {
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			}
		}
		// inputQueue = redisson.getQueue("MsgInputQueue");
	}

	public RedisTopicPublisher(String topic) {
		super();
		topicName = topic;
		publishTopic = redisson.getTopic(topic);
	}

	public void pushQueue(String payload) {
		try {
			// inputQueue.add(payload);
		} catch (Throwable e) {
			System.err.println(ExceptionHelper.getExceptionTrace(e));
		}
	}

	public void publishToTopic(String payload) {
		long clientsReceivedMessage = publishTopic.publish(payload);
		System.out.println("Published to topic " + topicName + " - " + clientsReceivedMessage);
	}
}
