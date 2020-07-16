package qslv.transfer.fulfillment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;
import qslv.util.ElapsedTimeSLILogger;

@Repository
public class KafkaDao {
	private static final Logger log = LoggerFactory.getLogger(KafkaDao.class);

	@Autowired
	private ConfigProperties config;
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ElapsedTimeSLILogger kafkaTimer;


	public void setConfig(ConfigProperties config) {
		this.config = config;
	}

	public void setKafkaTemplate(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void setKafkaTimer(ElapsedTimeSLILogger kafkaTimer) {
		this.kafkaTimer = kafkaTimer;
	}

	public void produceDLQMessage(TraceableMessage<TransferFulfillmentMessage> message) throws DataAccessException {
		log.trace("ENTRY produceDLQMessage");
		
		kafkaTimer.logElapsedTime(() -> {
		
			try {
				String messageJson = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(message);
				
				//TODO: retries across data centers
				// retries handled internally in kafka
				// wait with timemout for post to complete. 
				// kafkaTemplate auto-flush is set to true. timeouts are in properties.
	
				ProducerRecord<String ,String> record = 
					kafkaTemplate.send(config.getKafkaDeadLetterQueue(), message.getPayload().getFromAccountNumber(), messageJson)
					.get(config.getKafkaTimeout(), TimeUnit.MILLISECONDS).getProducerRecord();
					
				log.debug("Kakfa Produce {}", record.value());
			} catch ( ExecutionException ex) {
				log.debug(ex.getLocalizedMessage());
				throw new TransientDataAccessResourceException("Kafka Producer failure", ex);
			} catch ( TimeoutException | InterruptedException  ex) {
				log.debug(ex.getLocalizedMessage());
				throw new TransientDataAccessResourceException("Kafka Producer failure", ex);
			} catch (JsonProcessingException ex) {
				log.debug(ex.getLocalizedMessage());
				throw new NonTransientDataAccessResourceException("Jackson JSON object Mapper failed for Kafka Producer.", ex);
			}
		});
		
		// TODO: log time it took
		log.trace("EXIT produceDLQMessage");
	}
}
