package com.nextcentury.kairos.result.enqueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.nextcentury.kairos.utils.ExceptionHelper;

public class MsgEnqueuer {
	private static final Logger logger = LogManager.getLogger(MsgEnqueuer.class);
	private static final String redisHost = "redis://redis-cluster.kairos-redis:6379";
	private static RedissonClient redisson = null;
	private static RQueue<String> queue = null;
	static {
		if (redisson == null) {
			logger.info("Initializing redis cluster connection - redis cluster service: " + redisHost);
			try {
				Config config = new Config();
				config.useClusterServers().addNodeAddress(redisHost);
				redisson = Redisson.create(config);
			} catch (Throwable e) {
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			}
		}
	}

	public MsgEnqueuer() {
		super();
	}

	public void push(String qName, String payload) {
		queue = redisson.getQueue(qName);
		logger.info("Pushing result to queue - " + qName);
		logger.info(payload);
		if (!payload.isEmpty()) {
			try {
				queue.add(payload);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} else {
			logger.info("No-Op - empty payload");
		}
	}
}
