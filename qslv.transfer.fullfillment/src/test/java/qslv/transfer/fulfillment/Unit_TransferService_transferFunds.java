package qslv.transfer.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CommitReservationRequest;
import qslv.transaction.request.TransactionRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.CommitReservationResponse;
import qslv.transaction.response.TransactionResponse;
import qslv.transfer.request.TransferFulfillmentMessage;

@ExtendWith(MockitoExtension.class)
class Unit_TransferService_transferFunds {
	@Mock
	private TransactionDao transactionDao;
	@Mock
	private KafkaDao kafkaDao;

	FulfillmentService service = new FulfillmentService();

	@BeforeEach
	public void setup() {
		service.setKafkaDao(kafkaDao);
		service.setTransactionDao(transactionDao);
	}

	@Test
	void test_transferFunds_success() {

		// --Setup-------------
		TraceableMessage<TransferFulfillmentMessage> message = setup_message();

		// --Prepare-------------
		doReturn(setup_response()).when(transactionDao).recordTransaction(any(), any(TransactionRequest.class));
		doReturn(setup_commit()).when(transactionDao).commitReservation(any(), any(CommitReservationRequest.class));

		// --Execute-------------
		service.transferFunds(message);

		// --Verify-------------
		ArgumentCaptor<TransactionRequest> targ = ArgumentCaptor.forClass(TransactionRequest.class);
		ArgumentCaptor<CommitReservationRequest> carg = ArgumentCaptor.forClass(CommitReservationRequest.class);

		verify(transactionDao).recordTransaction(any(), targ.capture());
		verify(transactionDao).commitReservation(any(), carg.capture());

		assertEquals(message.getPayload().getToAccountNumber(), targ.getValue().getAccountNumber());
		assertNull(targ.getValue().getDebitCardNumber());
		assertEquals(message.getPayload().getRequestUuid(), targ.getValue().getRequestUuid());
		assertEquals(message.getPayload().getTransactionAmount(), targ.getValue().getTransactionAmount());
		assertEquals(message.getPayload().getTransactionMetaDataJson(), targ.getValue().getTransactionMetaDataJson());
		
		assertEquals(message.getPayload().getRequestUuid(), carg.getValue().getRequestUuid());
		assertEquals(message.getPayload().getReservationUuid(), carg.getValue().getReservationUuid());
		assertEquals(0L - message.getPayload().getTransactionAmount(), carg.getValue().getTransactionAmount());
		assertEquals(message.getPayload().getTransactionMetaDataJson(), carg.getValue().getTransactionMetaDataJson());

	}


	TransactionResponse setup_response() {
		TransactionResponse resourceResponse = new TransactionResponse(TransactionResponse.SUCCESS, new TransactionResource());
		resourceResponse.getTransactions().get(0).setAccountNumber("12345679");
		resourceResponse.getTransactions().get(0).setDebitCardNumber("7823478239467");
		return resourceResponse;
	}
	CommitReservationResponse setup_commit() {
		CommitReservationResponse response = new CommitReservationResponse(CommitReservationResponse.SUCCESS, new TransactionResource());
		response.getResource().setAccountNumber("82374827348273");
		response.getResource().setDebitCardNumber("829348298374293974");
		return response;
	}
	private TraceableMessage<TransferFulfillmentMessage> setup_message() {
		TraceableMessage<TransferFulfillmentMessage> message = new TraceableMessage<TransferFulfillmentMessage>();
		message.setBusinessTaxonomyId("Business Tax");
		message.setCorrelationId("correlation id");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("AIT 123");
		message.setPayload(new TransferFulfillmentMessage());
		message.getPayload().setFromAccountNumber("12345673234");
		message.getPayload().setRequestUuid(UUID.randomUUID());
		message.getPayload().setToAccountNumber("2374829347");
		message.getPayload().setReservationUuid(UUID.randomUUID());
		message.getPayload().setTransactionAmount(7232934L);
		message.getPayload().setTransactionMetaDataJson("{}");
		return message;
	}
}