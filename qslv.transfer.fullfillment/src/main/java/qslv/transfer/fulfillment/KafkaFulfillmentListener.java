package qslv.transfer.fulfillment;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;

@Component
public class KafkaFulfillmentListener {
	private static final Logger log = LoggerFactory.getLogger(KafkaFulfillmentListener.class);

	@Autowired
	private FulfillmentController fulfillmentController;

	public void setFulfillmentController(FulfillmentController fulfillmentController) {
		this.fulfillmentController = fulfillmentController;
	}

	@KafkaListener(topics = "#{@configProperties.kafkaTransferRequestQueue}")
	void onMessage(final ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data, Acknowledgment acknowledgment) {
		log.trace("onMessage ENTRY");

		fulfillmentController.fulfill(data, acknowledgment);

		log.trace("onMessage EXIT");
	}


}
