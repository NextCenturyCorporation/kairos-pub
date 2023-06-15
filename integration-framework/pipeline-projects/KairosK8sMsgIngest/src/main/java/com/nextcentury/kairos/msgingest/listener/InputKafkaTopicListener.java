package com.nextcentury.kairos.msgingest.listener;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.nextcentury.kairos.KafkaConfig;
import com.nextcentury.kairos.RedisTopicPublisher;
import com.nextcentury.kairos.ServiceInvocationConfig;
import com.nextcentury.kairos.performer.algorithm.entrypoint.io.Content;
import com.nextcentury.kairos.performer.algorithm.entrypoint.io.EntrypointMessage;
import com.nextcentury.kairos.utils.ExceptionHelper;

public class InputKafkaTopicListener {
	//private static final Logger logger = LogManager.getLogger(InputKafkaTopicListener.class);

	private static final String DASH = "-";
	private static final String ENCLAVE = "enclave";

	private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss.SSS");

	private static final Properties props = new Properties();
	private Consumer<String, String> consumer = null;

	public static String DEFAULT_KAFKACONFIG_EVENTCONSUMER_GROUP = "kairosEventConsumerGroup1";
	public static String DEFAULT_KAFKACONFIG_OFFSET_RESET_EARLIEST = "earliest";
	public static Integer DEFAULT_KAFKACONFIG_MAX_POLL_RECORDS = 1;

	private final KafkaConfig kafkaConfig;

	private final ServiceInvocationConfig svcInvocationConfig;
	private final String evaluatorName;
	private String experimentName;

	private ExecutorService executorService = null;

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Inclusion.NON_NULL);
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public InputKafkaTopicListener(String evaluatorName, String experimentName, KafkaConfig kafkaConfig,
			ExecutorService executorService, ServiceInvocationConfig svcInvocationConfig) {
		this.kafkaConfig = kafkaConfig;
		this.executorService = executorService;
		this.svcInvocationConfig = svcInvocationConfig;
		this.evaluatorName = evaluatorName;
		this.experimentName = experimentName;

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaConfig.getKafkaBrokers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_KAFKACONFIG_EVENTCONSUMER_GROUP);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, DEFAULT_KAFKACONFIG_MAX_POLL_RECORDS);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, DEFAULT_KAFKACONFIG_OFFSET_RESET_EARLIEST);
		props.put(ConsumerConfig.DEFAULT_ISOLATION_LEVEL, "read_committed");
	}

	public void runConsumer() {
		AtomicInteger counter = new AtomicInteger(0);

		System.out.println(" - Brokers - " + this.kafkaConfig.getKafkaBrokers());

		try {
			consumer = new KafkaConsumer<>(props);
			consumer.subscribe(Collections.singletonList(kafkaConfig.getInputKafkaTopic()));

			System.out.println("");
			System.out.println(" - Kairos Msg Ingestor service ready ......listening on topic  - "
					+ kafkaConfig.getInputKafkaTopic());
			System.out.println("");

			while (true) {
				ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
				// 1000 is the time in milliseconds consumer will wait if no record is found at
				// broker.
				if (consumerRecords.count() == 0) {
					System.out.println(" -  No messages found on " + kafkaConfig.getInputKafkaTopic() + ", returning....");
					continue;
				} else {
					for (TopicPartition partition : consumerRecords.partitions()) {
						List<ConsumerRecord<String, String>> partitionRecords = consumerRecords.records(partition);
						for (ConsumerRecord<String, String> record : partitionRecords) {
							System.out.println(
									"-----------------------------------------------------------------------------------------------");
							System.out.println("Retrieved record Key - " + record.key() + " from Kafka topic '"
									+ kafkaConfig.getInputKafkaTopic() + "'");
							System.out.println("Record value - " + record.value());
							System.out.println("Record partition " + record.partition());
							System.out.println("Record offset " + record.offset());
							System.out.println(
									"-----------------------------------------------------------------------------------------------");
							System.out.println("");

							// increment counter value
							counter.incrementAndGet();

							if (svcInvocationConfig.isKairosPerformer()) {
								System.out.println("Publishing message to redis topic - "
										+ svcInvocationConfig.getInputRedisTopic());
								// write the message to the redis input topic
								// process this message in a new thread
								executorService.submit(new KafkaRecordHandlerThread(record,
										new RedisTopicPublisher(svcInvocationConfig.getInputRedisTopic())));
							}

							if (svcInvocationConfig.isAidaPerformer()) {
								System.out.println("Publishing message to experiment input folder  - "
										+ svcInvocationConfig.getMountPathInput());
								// write the message to the input folder
								// process this message in a new thread
								executorService.submit(
										new KafkaRecordHandlerThread(record, svcInvocationConfig.getMountPathInput()));
							}
						}
						long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
						consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
					}
				}
			}
		} catch (Throwable e) {
			// e.printStackTrace();
			System.err.println(ExceptionHelper.getExceptionTrace(e));
		} finally {
			if (consumer != null) {
				System.out.println("Closing consumer ...");
				consumer.close();
			}

			System.out.println(String.format("Total records processed - %d%n\n", counter.intValue()));
		}
	}

	class KafkaRecordHandlerThread implements Runnable {
		private final ConsumerRecord<String, String> record;
		private RedisTopicPublisher redisTopicPublisher = null;
		private String mountPathInput = null;

		public KafkaRecordHandlerThread(ConsumerRecord<String, String> record) {
			this.record = record;
		}

		public KafkaRecordHandlerThread(ConsumerRecord<String, String> record,
				RedisTopicPublisher redisTopicPublisher) {
			this(record);
			this.redisTopicPublisher = redisTopicPublisher;
		}

		public KafkaRecordHandlerThread(ConsumerRecord<String, String> record, String mountPathInput) {
			this(record);
			this.mountPathInput = mountPathInput;
		}

		public void run() {
			if (redisTopicPublisher != null) {
				// publish to redis topic,
				// performers pick it up from there
				// we no longer need to re-decorate the raw message
				redisTopicPublisher.publishToTopic(record.value());
			}

			if (mountPathInput != null) {
				String inputFileName = mountPathInput + "/msg-" + Instant.now().toString();
				System.out.println("Writing to file - " + inputFileName);
				System.out.println(record.value());
				try {
					Files.write(Paths.get(inputFileName), buildKairosEntrypointMessage(record.value()).getBytes(),
							StandardOpenOption.CREATE_NEW);
				} catch (Throwable e) {
					System.err.println(ExceptionHelper.getExceptionTrace(e));
				}
			}
		}

		private String buildKairosEntrypointMessage(String rawMessage) {
			var enclaveName = (evaluatorName + DASH + experimentName + DASH +
					ENCLAVE).toLowerCase();
			String sender = (evaluatorName + DASH + experimentName)
					.toLowerCase();

			EntrypointMessage inputDataTuple = new EntrypointMessage();
			inputDataTuple.setTime(dateFormat.print(LocalDateTime.now(DateTimeZone.getDefault())));
			if (inputDataTuple.getId() == null || inputDataTuple.getId().isEmpty())
				inputDataTuple.setId(UUID.randomUUID().toString());

			// this is just testing, not sure what these values should be
			inputDataTuple.setSender(sender);

			Content content = new Content();
			content.setData(rawMessage);
			inputDataTuple.setContent(content);

			String payloadString = null;
			try {
				payloadString = new String(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(inputDataTuple));
			} catch (Throwable e) {
				System.err.println(ExceptionHelper.getExceptionTrace(e));
			}

			// log the message that was constructed by the framework
			System.out.println("Kairos Input Message - ");
			System.out.println(payloadString);
			System.out.println("");

			return payloadString;
		}
	}
}
