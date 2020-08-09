package qslv.transfer.fulfillment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Controller;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;
import qslv.transfer.response.TransferFulfillmentDeadLetter;
import qslv.util.EnableQuickSilver;
import qslv.util.LogKafkaTracingData;
import qslv.util.ServiceElapsedTimeSLI;
import qslv.util.ServiceLevelIndicator;

@Controller
@EnableQuickSilver
public class FulfillmentController {
	private static final Logger log = LoggerFactory.getLogger(FulfillmentController.class);

	@Autowired
	private ConfigProperties config;	
	@Autowired
	private FulfillmentService fulfillmentService;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setFulfillmentService(FulfillmentService fulfillmentService) {
		this.fulfillmentService = fulfillmentService;
	}

	@LogKafkaTracingData(value="TransferFulfillment::#{@configProperties.kafkaTransferRequestQueue}", ait="237482" )
	@ServiceElapsedTimeSLI(value="TransferFulfillment::onMessage", injectResponse = false, ait = "44444")
	public void fulfill(final ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data, Acknowledgment acknowledgment) {

		try {
			validateMessage(data.value());
			//TODO Log instrumentation
			//TODO time processing of message.

			validatePayload(data.value().getPayload());	
			fulfillmentService.transferFunds(data.value());

			LocalDateTime now = LocalDateTime.now();
			long elapsedSeconds = now.toEpochSecond(ZoneOffset.UTC) - data.value().getMessageCreationTime().toEpochSecond(ZoneOffset.UTC);
			ServiceLevelIndicator.logAsyncServiceElapsedTime(log, "TransferFulfillment", "2342", data.value().getMessageCreationTime(), 
					now, Duration.ofSeconds(elapsedSeconds));
		} catch (TransientDataAccessException ex) {
			log.warn("Recoverable error. Return message to Kafka and sleep for {} ms.", config.getKafkaTimeout());
			acknowledgment.nack(10000L);
			return;			
		} catch (Exception ex) {
			log.error("Unexpected exception thrown. Sending to DLQ. {}", ex.getLocalizedMessage());
			try {
				fulfillmentService.sendToDeadLetterQueue(new TransferFulfillmentDeadLetter(data.value().getPayload(), ex));
			} catch (Exception iex) {
				log.error("Unexpected exception while sending to DLQ. Keep on Kafka. {}", iex.getLocalizedMessage());
				acknowledgment.nack(10000L);
				return;	
			}
		}

		acknowledgment.acknowledge();
	}

	private void validatePayload(TransferFulfillmentMessage payload) throws NonTransientDataAccessResourceException {
		if (payload.getVersion() == null || false == payload.getVersion().equals(TransferFulfillmentMessage.version1_0)) {
			throw new NonTransientDataAccessResourceException("Malformed message. Invalid version.");
		}
		if (payload.getFromAccountNumber() == null || payload.getFromAccountNumber().isEmpty()) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Missing From Account Number.");
		}
		if (payload.getToAccountNumber() == null || payload.getToAccountNumber().isEmpty()) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Missing From To Number.");
		}
		if (payload.getRequestUuid() == null) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Missing From Request UUID.");
		}
		if (payload.getReservationUuid() == null) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Missing From Reservation UUID.");
		}
		if (payload.getTransactionAmount() <= 0) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Amount less than or equal to 0.");
		}
		if (payload.getTransactionMetaDataJson() == null || payload.getTransactionMetaDataJson().isEmpty()) {
			throw new NonTransientDataAccessResourceException("Malformed message payload. Missing Meta Data.");
		}
	}

	private void validateMessage(TraceableMessage<?> data) throws NonTransientDataAccessResourceException {
		if (null == data.getProducerAit()) {
			throw new NonTransientDataAccessResourceException("Malformed message. Missing Producer AIT Id.");
		}
		if (null == data.getCorrelationId()) {
			throw new NonTransientDataAccessResourceException("Malformed message. Missing Correlation Id.");
		}
		if (null == data.getBusinessTaxonomyId()) {
			throw new NonTransientDataAccessResourceException("Malformed message. Missing Business Taxonomy Id.");
		}
		if (null == data.getMessageCreationTime()) {
			throw new NonTransientDataAccessResourceException("Malformed message. Missing Message Creation Time.");
		}
		if (null == data.getPayload()) {
			throw new NonTransientDataAccessResourceException("Malformed message. Missing Fulfillment Message.");
		}
	}

}
