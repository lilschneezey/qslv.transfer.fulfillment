package qslv.transfer.fulfillment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.support.Acknowledgment;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;


@ExtendWith(MockitoExtension.class)
@SpringBootTest
class Unit_FulfillmentController_postTransferFunds {
	@Autowired
	FulfillmentController controller;
	@Mock
	public FulfillmentService fulfillmentService;
	@Mock 
	Acknowledgment acknowledgment;
	@Captor
	ArgumentCaptor<TraceableMessage<TransferFulfillmentMessage>> captor;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		controller.setFulfillmentService(fulfillmentService);
	}

	@Test
	void test_onMessage_success() {
		//-- Setup ------------------
		ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data = setup_response();

		//--Prepare----------------------
		doNothing().when(fulfillmentService).transferFunds(any());
		doNothing().when(acknowledgment).acknowledge();
		
		//--Execute-----------------------
		controller.fulfill(data, acknowledgment);
		
		//--Verify------------------------
		verify(fulfillmentService).transferFunds(captor.capture());
		verify(acknowledgment).acknowledge();
	}
	
	@Test
	void test_onMessage_transferFundsFails() {
		//-- Setup ------------------
		ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data = setup_response();

		//--Prepare----------------------
		doThrow(new NonTransientDataAccessResourceException("msg")).when(fulfillmentService).transferFunds(any());
		doNothing().when(fulfillmentService).sendToDeadLetterQueue(any());
		doNothing().when(acknowledgment).acknowledge();
		
		//--Execute-----------------------
		controller.fulfill(data, acknowledgment);
		
		//--Verify------------------------
		verify(fulfillmentService).transferFunds(captor.capture());
		verify(fulfillmentService).sendToDeadLetterQueue(any());
		verify(acknowledgment).acknowledge();
	}
	
	@Test
	void test_onMessage_transferFundsTimesOut() {
		//-- Setup ------------------
		ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data = setup_response();

		//--Prepare----------------------
		doThrow(new TransientDataAccessResourceException("msg")).when(fulfillmentService).transferFunds(any());
		doNothing().when(acknowledgment).nack(anyLong());
		
		//--Execute-----------------------
		controller.fulfill(data, acknowledgment);
		
		//--Verify------------------------
		verify(fulfillmentService).transferFunds(captor.capture());
		verify(acknowledgment).nack(anyLong());
	}

	@Test
	void test_onMessage_dlqFails() {
		//-- Setup ------------------
		ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data = setup_response();
		data.value().getPayload().setVersion("237482734982");

		//--Prepare----------------------
		doThrow(new TransientDataAccessResourceException("msg")).when(fulfillmentService).sendToDeadLetterQueue(any());
		doNothing().when(acknowledgment).nack(anyLong());
		
		//--Execute-----------------------
		controller.fulfill(data, acknowledgment);
		
		//--Verify------------------------
		verify(fulfillmentService).sendToDeadLetterQueue(any());
		verify(acknowledgment).nack(anyLong());
	}
	
	@Test
	void test_onMessage_validateInput() {
		int count = 0;

		//-- Prepare ------------------
		doNothing().when(fulfillmentService).sendToDeadLetterQueue(any());
		doNothing().when(acknowledgment).acknowledge();

		//-- Setup ------------------
		ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> data = setup_response();

		//-- TraceableMessage ------------------

		data.value().setProducerAit(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().setProducerAit("23423");
		data.value().setBusinessTaxonomyId(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().setBusinessTaxonomyId("823942.8293423.2342");
		data.value().setCorrelationId(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().setCorrelationId("23874293742893");
		data.value().setMessageCreationTime(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().setMessageCreationTime(LocalDateTime.now());
		data.value().getPayload().setVersion(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setVersion(TransferFulfillmentMessage.version1_0);
		TransferFulfillmentMessage holdvalue = data.value().getPayload();
		data.value().setPayload(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().setPayload(holdvalue);
		
		//-- Payload ------------------
		data.value().getPayload().setFromAccountNumber(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setFromAccountNumber("2389420834089234");
		data.value().getPayload().setRequestUuid(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setRequestUuid(UUID.randomUUID());
		data.value().getPayload().setReservationUuid(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setReservationUuid(UUID.randomUUID());
		data.value().getPayload().setToAccountNumber(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setToAccountNumber("2389420834089234");
		data.value().getPayload().setTransactionAmount(0L);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setTransactionAmount(-283948L);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setTransactionAmount(238942L);
		data.value().getPayload().setTransactionMetaDataJson(null);
		controller.fulfill(data, acknowledgment); count++;
		verify(fulfillmentService, times(count)).sendToDeadLetterQueue(any());
		verify(acknowledgment, times(count)).acknowledge();
		
		data.value().getPayload().setTransactionMetaDataJson("2389420834089234");

		//--Prepare----------------------
		doNothing().when(fulfillmentService).transferFunds(any());
		doNothing().when(acknowledgment).acknowledge();
		
		//--Execute-----------------------
		controller.fulfill(data, acknowledgment); count++;
				
		//--Verify------------------------
		verify(fulfillmentService).transferFunds(captor.capture());
		verify(acknowledgment, times(count)).acknowledge();
	}
	
	ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>> setup_response() {
		TraceableMessage<TransferFulfillmentMessage> message = new TraceableMessage<TransferFulfillmentMessage>();
		message.setBusinessTaxonomyId("28934.82934.89234");
		message.setCorrelationId("2u3472938482374982");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("272834");
		
		message.setPayload(new TransferFulfillmentMessage());
		message.getPayload().setVersion(TransferFulfillmentMessage.version1_0);
		message.getPayload().setFromAccountNumber("709789345");
		message.getPayload().setRequestUuid(UUID.randomUUID());
		message.getPayload().setReservationUuid(UUID.randomUUID());
		message.getPayload().setToAccountNumber("238492834");
		message.getPayload().setTransactionAmount(8923489);
		message.getPayload().setTransactionMetaDataJson("{}");
		return new ConsumerRecord<String, TraceableMessage<TransferFulfillmentMessage>>("blah", 1, 1, "234234", message);
	}

}
