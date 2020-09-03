package qslv.transfer.fulfillment;

import static org.junit.jupiter.api.Assertions.*;
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
import qslv.transaction.request.CommitReservationRequest;
import qslv.transaction.request.TransactionRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.CommitReservationResponse;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@EnableRetry
class Unit_TransactionDao_commitReservation {
	
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
		config.setCommitReservationUrl("http://localhost:9091/CommitTransaction");
		config.setAitid("TEST AIT");
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
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResource>>>any());
		
		CommitReservationResponse callresult = transactionDao.commitReservation(message, request);
		assertEquals(CommitReservationResponse.SUCCESS, callresult.getStatus());
		assertEquals(resourceResponse.getResource().getAccountNumber(), callresult.getResource().getAccountNumber());
		assertEquals(resourceResponse.getResource().getDebitCardNumber(), callresult.getResource().getDebitCardNumber());
	}

	CommitReservationRequest request;
	TraceableMessage<CommitReservationRequest> message;
	
	void setup_request() {
		request = new CommitReservationRequest();
		request.setReservationUuid(UUID.randomUUID());
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		message = new TraceableMessage<CommitReservationRequest>();
		message.setBusinessTaxonomyId("jskdfjsdjfls");
		message.setCorrelationId("sdjfsjdlfjslkdfj");
		message.setMessageCreationTime(LocalDateTime.now());
		message.setProducerAit("234234");
		message.setPayload(request);
	}
	
	CommitReservationResponse resourceResponse;
	ResponseEntity<TimedResponse<CommitReservationResponse>> response;
	void setup_response() {
		resourceResponse = new CommitReservationResponse(CommitReservationResponse.SUCCESS, new TransactionResource());
		resourceResponse.getResource().setAccountNumber("12345679");
		resourceResponse.getResource().setDebitCardNumber("7823478239467");
		response = new ResponseEntity<TimedResponse<CommitReservationResponse>>(new TimedResponse<>(123456L, resourceResponse), HttpStatus.CREATED);
	}
	void setup_failedResponse() {
		resourceResponse = new CommitReservationResponse(CommitReservationResponse.SUCCESS, new TransactionResource());
		resourceResponse.getResource().setAccountNumber("12345679");
		resourceResponse.getResource().setDebitCardNumber("7823478239467");
		response = new ResponseEntity<TimedResponse<CommitReservationResponse>>(new TimedResponse<>(234567L, resourceResponse), HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@Test
	void test_recordReservation_failsOnce() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CommitReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);

		CommitReservationResponse callresult = transactionDao.commitReservation(message, request);
		assert(callresult.getStatus() == CommitReservationResponse.SUCCESS);
		assertEquals(resourceResponse.getResource().getAccountNumber(), callresult.getResource().getAccountNumber());
		assertEquals(resourceResponse.getResource().getDebitCardNumber(), callresult.getResource().getDebitCardNumber());

	}

	@Test
	void test_recordReservation_failsTwice() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CommitReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);
		
		CommitReservationResponse callresult = transactionDao.commitReservation(message, request);
		assertTrue(callresult.getStatus() == CommitReservationResponse.SUCCESS);
		assertEquals(resourceResponse.getResource().getAccountNumber(), callresult.getResource().getAccountNumber());
		assertEquals(resourceResponse.getResource().getDebitCardNumber(), callresult.getResource().getDebitCardNumber());
	}
	
	@Test
	void test_recordReservation_failsThrice() {
		
		setup_request();
		setup_response();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CommitReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) );
		
		assertThrows(TransientDataAccessResourceException.class, () -> {
			transactionDao.commitReservation(message, request);
		});

	}
	
	@Test
	void test_recordReservation_serverFailure() {
		
		setup_request();
		setup_failedResponse();
		
		//-----------------
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<TransactionRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<CommitReservationResponse>>>any()))
			.thenReturn(response);
		
		assertThrows(NonTransientDataAccessResourceException.class, () -> {
			transactionDao.commitReservation(message, request);
		});

	}
}
