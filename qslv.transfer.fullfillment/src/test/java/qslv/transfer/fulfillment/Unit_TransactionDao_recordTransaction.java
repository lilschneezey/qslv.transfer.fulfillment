package qslv.transfer.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import qslv.common.TimedResponse;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.TransactionRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.TransactionResponse;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@EnableRetry
class Unit_TransactionDao_recordTransaction {
	
	@Mock
	RestTemplate restTemplate;
	@Autowired
	RestTemplateProxy restTemplateProxy;
	@Autowired
	ConfigProperties config;

	@Autowired
	TransactionDao transactionDao;
	
	@BeforeEach
	public void init() {
		config.setAitid("723842");
		config.setPostTransactionUrl("http://localhost:9091/Transaction");
		transactionDao.setConfig(config);
		restTemplateProxy.setRestTemplate(restTemplate);
	}
	
	@Test
	void test_recordTransaction_success() {
		
		setup_request();
		setup_response();
		
		//-Prepare----------------
		doReturn(response).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResponse>>>any());
		
		TransactionResponse callresult = transactionDao.recordTransaction(message, request);
		assertEquals(TransactionResponse.SUCCESS, callresult.getStatus());
		assertEquals(resourceResponse.getTransactions().get(0).getAccountNumber(), callresult.getTransactions().get(0).getAccountNumber());
		assertEquals(resourceResponse.getTransactions().get(0).getDebitCardNumber(), callresult.getTransactions().get(0).getDebitCardNumber());
	}

	TransactionRequest request;
	TraceableMessage<TransactionRequest> message;
	
	void setup_request() {
		request = new TransactionRequest();
		request.setAccountNumber("237489237492");
		request.setDebitCardNumber("1234HHHH1234JJJJ");
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		message = new TraceableMessage<TransactionRequest>();
		message.setBusinessTaxonomyId("jskdfjsdjfls");
		message.setCorrelationId("sdjfsjdlfjslkdfj");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("234234");
		message.setPayload(request);
	}
	
	TransactionResponse resourceResponse;
	ResponseEntity<TimedResponse<TransactionResponse>> response;
	void setup_response() {
		TransactionResource transaction = new TransactionResource();
		transaction.setAccountNumber("12345679");
		transaction.setDebitCardNumber("7823478239467");
		resourceResponse = new TransactionResponse(TransactionResponse.SUCCESS, transaction);
		response = new ResponseEntity<TimedResponse<TransactionResponse>>(new TimedResponse<>(123456L, resourceResponse), HttpStatus.CREATED);
	}
	
	void setup_failedResponse() {
		TransactionResource transaction = new TransactionResource();
		transaction.setAccountNumber("12345679");
		transaction.setDebitCardNumber("7823478239467");
		resourceResponse = new TransactionResponse(TransactionResponse.SUCCESS, transaction);
		response = new ResponseEntity<TimedResponse<TransactionResponse>>(new TimedResponse<>(765123L, resourceResponse), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@Test
	void test_recordReservation_failsOnce() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);

		TransactionResponse callresult = transactionDao.recordTransaction(message, request);
		assert(callresult.getStatus() == TransactionResponse.SUCCESS);
		assertEquals(resourceResponse.getTransactions().get(0).getAccountNumber(), callresult.getTransactions().get(0).getAccountNumber());
		assertEquals(resourceResponse.getTransactions().get(0).getDebitCardNumber(), callresult.getTransactions().get(0).getDebitCardNumber());

	}

	@Test
	void test_recordReservation_failsTwice() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);
		
		TransactionResponse callresult = transactionDao.recordTransaction(message, request);
		assertTrue(callresult.getStatus() == TransactionResponse.SUCCESS);
		assertEquals(resourceResponse.getTransactions().get(0).getAccountNumber(), callresult.getTransactions().get(0).getAccountNumber());
		assertEquals(resourceResponse.getTransactions().get(0).getDebitCardNumber(), callresult.getTransactions().get(0).getDebitCardNumber());
	}
	
	@Test
	void test_recordReservation_failsThrice() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResource>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) );
		
		assertThrows(TransientDataAccessResourceException.class, () -> {
			transactionDao.recordTransaction(message, request);
		});

	}
	
	@Test
	void test_recordReservation_serverFailure() {
		
		setup_request();
		setup_failedResponse();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResponse>>>any()))
			.thenReturn(response);
		
		assertThrows(NonTransientDataAccessResourceException.class, () -> {
			transactionDao.recordTransaction(message, request);
		});

	}
}
