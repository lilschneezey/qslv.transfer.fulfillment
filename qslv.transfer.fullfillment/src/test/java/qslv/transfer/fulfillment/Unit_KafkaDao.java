package qslv.transfer.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;
import qslv.util.ElapsedTimeSLILogger;

@ExtendWith(MockitoExtension.class)
public class Unit_KafkaDao {
	KafkaDao kafkaDao = new KafkaDao();
	ConfigProperties config = new ConfigProperties();
	ElapsedTimeSLILogger kafkaTimer = new ElapsedTimeSLILogger(LoggerFactory.getLogger(KafkaDao.class), "2342342", "kafkaQueue", 
				Collections.singletonList(TransientDataAccessResourceException.class)); 
	
	@Mock
	KafkaTemplate<String, String> kafkaTemplate;
	@Mock
	ListenableFuture<SendResult<String, String>> future;
	@Mock 
	ObjectMapper mockMapper;
	ObjectMapper objectMapper = new ObjectMapper();

	{
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}
	@BeforeEach
	public void setup() {
		kafkaDao.setKafkaTemplate(kafkaTemplate);
		kafkaDao.setConfig(config);
		kafkaDao.setObjectMapper(objectMapper);
		kafkaDao.setKafkaTimer(kafkaTimer);
		config.setKafkaDeadLetterQueue("dead.test.letter.queue");
		config.setAitid("234234");
		config.setKafkaTimeout(23423);
	}
	
	@Test
	public void test_produceTransferMessage_Success() throws InterruptedException, ExecutionException, TimeoutException, JsonMappingException, JsonProcessingException {
		
		//-Prepare---------------
		kafkaDao.setObjectMapper(objectMapper);
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>("mockTopicName", "message");
		SendResult<String, String> sendResult = new SendResult<String, String>(producerRecord, new RecordMetadata(new TopicPartition("mockTopic", 1), 1, 1, 1, 1L, 1, 1));
		when( future.get(anyLong(), any(TimeUnit.class) ) ).thenReturn(sendResult);
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
	
		//--Setup ---------------------------
		TraceableMessage<TransferFulfillmentMessage> message = setup_message();

		//-Execute----------------------------		
		kafkaDao.produceDLQMessage(message);
		
		//-Verify----------------------------		
		verify(future).get(anyLong(), any(TimeUnit.class) );
		ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
		verify(kafkaTemplate).send(anyString(), anyString(), arg.capture());
		TraceableMessage<TransferFulfillmentMessage> result = objectMapper.readValue(arg.getValue(), new TypeReference<TraceableMessage<TransferFulfillmentMessage>>() {});
		
		assertNotNull(result);
		assertEquals(message.getBusinessTaxonomyId(), result.getBusinessTaxonomyId());
		assertEquals(message.getCorrelationId(), result.getCorrelationId());
		assertEquals(message.getMessageCreationTime(), result.getMessageCreationTime());
		assertEquals(message.getProducerAit(), result.getProducerAit());

		assertNotNull(result.getPayload());
		assertEquals(message.getPayload().getVersion(), result.getPayload().getVersion());
		assertEquals(message.getPayload().getFromAccountNumber(), result.getPayload().getFromAccountNumber());
		assertEquals(message.getPayload().getRequestUuid(), result.getPayload().getRequestUuid());
		assertEquals(message.getPayload().getToAccountNumber(), result.getPayload().getToAccountNumber());
		assertEquals(message.getPayload().getReservationUuid(), result.getPayload().getReservationUuid());
		assertEquals(message.getPayload().getTransactionAmount(), result.getPayload().getTransactionAmount());
		assertEquals(message.getPayload().getTransactionMetaDataJson(), result.getPayload().getTransactionMetaDataJson());
	}
	
	@Test
	public void test_produceTransferMessage_throwsTransient() throws InterruptedException, ExecutionException, TimeoutException {
		
		//-Prepare---------------
		kafkaDao.setObjectMapper(objectMapper);
		when( future.get(anyLong(), any(TimeUnit.class) ) )
			.thenThrow(new InterruptedException());
		when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
	
		//--Setup ---------------------------
		TraceableMessage<TransferFulfillmentMessage> message = setup_message();
		
		//--Execute--------------	
		assertThrows(TransientDataAccessResourceException.class, () -> {
			kafkaDao.produceDLQMessage(message);
		});
	}

	class TestException extends JsonProcessingException {
		private static final long serialVersionUID = 1L;
		protected TestException(String msg) {
			super(msg);
		}}
	@Mock com.fasterxml.jackson.databind.ObjectWriter objWriter;

	@Test
	public void test_produceTransferMessage_throwsNontransient() throws InterruptedException, ExecutionException, TimeoutException, JsonProcessingException {
		
		//-Prepare---------------
		kafkaDao.setObjectMapper(mockMapper);
		doReturn(objWriter).when(objWriter).withDefaultPrettyPrinter();
		doThrow(new TestException("message")).when(objWriter).writeValueAsString(any());
		doReturn(objWriter).when(mockMapper).writer();
	
		//--Setup ---------------------------
		TraceableMessage<TransferFulfillmentMessage> message = setup_message();
		
		//--Execute--------------	
		assertThrows(NonTransientDataAccessResourceException.class, () -> {
			kafkaDao.produceDLQMessage(message);
		});
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
