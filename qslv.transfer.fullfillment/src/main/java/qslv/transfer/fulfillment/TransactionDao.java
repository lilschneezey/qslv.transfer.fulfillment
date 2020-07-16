package qslv.transfer.fulfillment;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.common.kafka.TraceableMessage;
import qslv.transaction.request.CommitReservationRequest;
import qslv.transaction.request.TransactionRequest;
import qslv.transaction.response.CommitReservationResponse;
import qslv.transaction.response.TransactionResponse;
import qslv.util.ElapsedTimeSLILogger;

@Repository
public class TransactionDao {
	private static final Logger log = LoggerFactory.getLogger(TransactionDao.class);
	private static ParameterizedTypeReference<TimedResponse<TransactionResponse>> transactionResponseType =
			new ParameterizedTypeReference<TimedResponse<TransactionResponse>>() {};
	private static ParameterizedTypeReference<TimedResponse<CommitReservationResponse>> commitResponseType =
			new ParameterizedTypeReference<TimedResponse<CommitReservationResponse>>() {};

	@Autowired
	private ConfigProperties config;
	
	@Autowired
	private RestTemplateProxy restTemplateProxy;
	@Autowired
	private RetryTemplate retryTemplate;
	@Autowired
	ElapsedTimeSLILogger transactionTimer;
	@Autowired
	ElapsedTimeSLILogger commitTimer;
	
	public ConfigProperties getConfig() {
		return config;
	}

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}

	public void setRestTemplateProxy(RestTemplateProxy restTemplateProxy) {
		this.restTemplateProxy = restTemplateProxy;
	}
	
	public TransactionResponse recordTransaction(final TraceableMessage<?> message,final TransactionRequest request) {

		log.debug("recordTransaction ENTRY {}", request.toString());

		HttpHeaders headers = buildHeaders(message);
		long start = System.nanoTime();
		ResponseEntity<TimedResponse<TransactionResponse>> response = transactionTimer.logElapsedTime(() -> {
			try {
				return retryTemplate.execute(new RetryCallback<ResponseEntity<TimedResponse<TransactionResponse>>, ResourceAccessException>() {
					public ResponseEntity<TimedResponse<TransactionResponse>> doWithRetry( RetryContext context) throws ResourceAccessException {
						return restTemplateProxy.exchange(config.getPostTransactionUrl(), 
								HttpMethod.POST,
								new HttpEntity<TransactionRequest>(request, headers), 
								transactionResponseType
								);
					}
				});
			}
			catch (ResourceAccessException ex) {
				String msg = String.format("Exhausted %d retries for POST %s.", config.getRestAttempts(), config.getPostTransactionUrl());
				log.warn(msg);
				throw new TransientDataAccessResourceException(msg,ex);
			}
			catch (Exception ex) {
				log.info("{} URL {} FAIL AIT-ID: {} Client Elapsed-Time: {}", HttpMethod.POST, config.getPostTransactionUrl(), 
						config.getAitid(), (System.nanoTime() - start));
				log.debug(ex.getLocalizedMessage());
				throw (ex);
			}
		});
		if (	false == response.hasBody() 
				|| false == response.getStatusCode().equals(HttpStatus.CREATED)
				|| (response.getBody().getPayload().getStatus()) != TransactionResponse.SUCCESS
					&& response.getBody().getPayload().getStatus() != TransactionResponse.ALREADY_PRESENT ) {
			String msg = String.format("Unexpected return from %s Rest Service. %s", config.getPostTransactionUrl(), response.toString());
			log.error(msg);
			throw new NonTransientDataAccessResourceException(msg);
		}
		log.info("{} URL {} SUCCESS AIT-ID: {} Client Elapsed-Time: {} Server Elapsed-Time {}", HttpMethod.POST, config.getPostTransactionUrl(), 
				config.getAitid(), (System.nanoTime() - start), response.getBody().getServiceTimeElapsed());
		return response.getBody().getPayload();
	}
	
	public CommitReservationResponse commitReservation(final TraceableMessage<?> message, CommitReservationRequest request) {
		
		log.debug("recordTransaction ENTRY {}", request.toString());

		HttpHeaders headers = buildHeaders(message);
		long start = System.nanoTime();
		ResponseEntity<TimedResponse<CommitReservationResponse>> response = transactionTimer.logElapsedTime(() -> {
			try {
				return retryTemplate.execute(new RetryCallback<ResponseEntity<TimedResponse<CommitReservationResponse>>, ResourceAccessException>() {
					public ResponseEntity<TimedResponse<CommitReservationResponse>> doWithRetry( RetryContext context) throws ResourceAccessException {
						return restTemplateProxy.exchange(config.getCommitReservationUrl(), HttpMethod.POST,
								new HttpEntity<CommitReservationRequest>(request, headers), 
								commitResponseType);
					}
				});
			}
			catch (ResourceAccessException ex) {
				String msg = String.format("Exhausted %d retries for POST %s.", config.getRestAttempts(), config.getCommitReservationUrl());
				log.warn(msg);
				throw new TransientDataAccessResourceException(msg,ex);
			} catch (Exception ex) {
				log.info("{} URL {} FAIL AIT-ID: {} Client Elapsed-Time: {}", HttpMethod.POST, config.getCommitReservationUrl(), 
						config.getAitid(), (System.nanoTime() - start));
				log.debug(ex.getLocalizedMessage());
				throw (ex);
			}
		});
		
		if (false == response.hasBody() 
				|| false == response.getStatusCode().equals(HttpStatus.CREATED)
				|| (response.getBody().getPayload().getStatus() != TransactionResponse.SUCCESS
					&& response.getBody().getPayload().getStatus() != TransactionResponse.ALREADY_PRESENT )) {
			String msg = String.format("Unexpected return from %s Service. %s", config.getCommitReservationUrl(), response.toString());
			log.error(msg);
			throw new NonTransientDataAccessResourceException(msg);
		}
		log.info("{} URL {} SUCCESS AIT-ID: {} Client Elapsed-Time: {} Server Elapsed-Time {}", HttpMethod.POST, config.getCommitReservationUrl(), 
				config.getAitid(), (System.nanoTime() - start), response.getBody().getServiceTimeElapsed());
		return response.getBody().getPayload();
	}
	
	private HttpHeaders buildHeaders(final TraceableMessage<?> message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON) );
		headers.add(TraceableRequest.AIT_ID, config.getAitid());
		headers.add(TraceableRequest.BUSINESS_TAXONOMY_ID, message.getBusinessTaxonomyId());
		headers.add(TraceableRequest.CORRELATION_ID, message.getCorrelationId());
		return headers;
	}

}