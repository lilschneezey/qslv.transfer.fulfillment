package qslv.transfer.fulfillment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qslv")
public class ConfigProperties {

	private String aitid;
	private String postTransactionUrl;
	private String commitReservationUrl;
	private int restConnectionRequestTimeout = 1000;
	private int restConnectTimeout = 1000;
	private int restTimeout = 1000;
	private int restAttempts = 3;
	private int restBackoffDelay = 100;
	private int restBackoffDelayMax = 500; 
	private String kafkaTransferRequestQueue;
	private String kafkaDeadLetterQueue;
	private int kafkaTimeout;
	private String kafkaConsumerPropertiesPath;
	private String kafkaProducerPropertiesPath;

	public String getAitid() {
		return aitid;
	}

	public void setAitid(String aitid) {
		this.aitid = aitid;
	}

	public String getPostTransactionUrl() {
		return postTransactionUrl;
	}

	public void setPostTransactionUrl(String postTransactionUrl) {
		this.postTransactionUrl = postTransactionUrl;
	}

	public String getCommitReservationUrl() {
		return commitReservationUrl;
	}

	public void setCommitReservationUrl(String commitReservationUrl) {
		this.commitReservationUrl = commitReservationUrl;
	}

	public int getRestConnectionRequestTimeout() {
		return restConnectionRequestTimeout;
	}

	public void setRestConnectionRequestTimeout(int restConnectionRequestTimeout) {
		this.restConnectionRequestTimeout = restConnectionRequestTimeout;
	}

	public int getRestConnectTimeout() {
		return restConnectTimeout;
	}

	public void setRestConnectTimeout(int restConnectTimeout) {
		this.restConnectTimeout = restConnectTimeout;
	}

	public int getRestTimeout() {
		return restTimeout;
	}

	public void setRestTimeout(int restTimeout) {
		this.restTimeout = restTimeout;
	}

	public int getRestAttempts() {
		return restAttempts;
	}

	public void setRestAttempts(int restAttempts) {
		this.restAttempts = restAttempts;
	}

	public int getRestBackoffDelay() {
		return restBackoffDelay;
	}

	public void setRestBackoffDelay(int restBackoffDelay) {
		this.restBackoffDelay = restBackoffDelay;
	}

	public int getRestBackoffDelayMax() {
		return restBackoffDelayMax;
	}

	public void setRestBackoffDelayMax(int restBackoffDelayMax) {
		this.restBackoffDelayMax = restBackoffDelayMax;
	}

	public String getKafkaTransferRequestQueue() {
		return kafkaTransferRequestQueue;
	}

	public void setKafkaTransferRequestQueue(String kafkaTransferRequestQueue) {
		this.kafkaTransferRequestQueue = kafkaTransferRequestQueue;
	}

	public String getKafkaDeadLetterQueue() {
		return kafkaDeadLetterQueue;
	}

	public void setKafkaDeadLetterQueue(String kafkaDeadLetterQueue) {
		this.kafkaDeadLetterQueue = kafkaDeadLetterQueue;
	}

	public String getKafkaConsumerPropertiesPath() {
		return kafkaConsumerPropertiesPath;
	}

	public void setKafkaConsumerPropertiesPath(String kafkaConsumerPropertiesPath) {
		this.kafkaConsumerPropertiesPath = kafkaConsumerPropertiesPath;
	}

	public String getKafkaProducerPropertiesPath() {
		return kafkaProducerPropertiesPath;
	}

	public void setKafkaProducerPropertiesPath(String kafkaProducerPropertiesPath) {
		this.kafkaProducerPropertiesPath = kafkaProducerPropertiesPath;
	}

	public int getKafkaTimeout() {
		return kafkaTimeout;
	}

	public void setKafkaTimeout(int kafkaTimeout) {
		this.kafkaTimeout = kafkaTimeout;
	}
	
}
