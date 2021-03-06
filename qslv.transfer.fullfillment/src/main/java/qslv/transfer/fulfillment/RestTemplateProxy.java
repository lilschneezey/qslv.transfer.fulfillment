package qslv.transfer.fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import qslv.util.RemoteServiceSLI;

@Component
public class RestTemplateProxy  {
	@Autowired
	RestTemplate restTemplate;

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@RemoteServiceSLI(value="POST /Transaction", ait="#{@configProperties.aitid}", remoteAit="#{@configProperties.aitid}", remoteFailures= {ResourceAccessException.class})
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType, Object... uriVariables) throws RestClientException {
		return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
	}
	@RemoteServiceSLI(value="POST /Transaction", ait="#{@configProperties.aitid}", remoteAit="#{@configProperties.aitid}", remoteFailures= {ResourceAccessException.class})
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException {
		return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
	}
	
}
