package qslv.transfer.fulfillment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CommitReservationRequest;
import qslv.transaction.request.TransactionRequest;
import qslv.transaction.response.CommitReservationResponse;
import qslv.transaction.response.TransactionResponse;
import qslv.transfer.request.TransferFulfillmentMessage;
import qslv.transfer.response.TransferFulfillmentDeadLetter;

@Service
public class FulfillmentService {
	private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

	@Autowired
	private TransactionDao transactionDao;
	@Autowired
	private KafkaDao kafkaDao;

	public TransactionDao getTransactionDao() {
		return transactionDao;
	}

	public void setTransactionDao(TransactionDao transactionDao) {
		this.transactionDao = transactionDao;
	}

	public KafkaDao getKafkaDao() {
		return kafkaDao;
	}

	public void setKafkaDao(KafkaDao kafkaDao) {
		this.kafkaDao = kafkaDao;
	}

	public void transferFunds(final TraceableMessage<TransferFulfillmentMessage> message) {
		log.trace("service.transferFunds ENTRY");

		// ---Move money to account first-------------
		TransactionRequest request = new TransactionRequest();
		request.setAccountNumber(message.getPayload().getToAccountNumber());
		request.setDebitCardNumber(null);
		request.setRequestUuid(message.getPayload().getRequestUuid());
		request.setTransactionAmount(message.getPayload().getTransactionAmount());
		request.setTransactionMetaDataJson(message.getPayload().getTransactionMetaDataJson());

		TransactionResponse transferResponse = transactionDao.recordTransaction(message, request);
		log.debug(transferResponse.toString());

		// ---commit reservation in from account second-------------
		CommitReservationRequest commit = new CommitReservationRequest();
		commit.setRequestUuid(message.getPayload().getRequestUuid());
		commit.setReservationUuid(message.getPayload().getReservationUuid());
		commit.setTransactionAmount(0L - message.getPayload().getTransactionAmount());
		commit.setTransactionMetaDataJson(message.getPayload().getTransactionMetaDataJson());

		CommitReservationResponse commitResponse = transactionDao.commitReservation(message, commit);
		log.debug(commitResponse.toString());
	
		log.trace("service.transferFunds EXIT");
	}
	
	public void sendToDeadLetterQueue(final TransferFulfillmentDeadLetter message) {
		log.trace("service.sendToDeadLetterQueue ENTRY");
		kafkaDao.produceDLQMessage(message);
		log.trace("service.sendToDeadLetterQueue EXIT");

	}
}