package qslv.transfer.fulfillment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import qslv.common.kafka.JacksonAvroDeserializer;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;
import qslv.util.ElapsedTimeSLILogger;

@Configuration
public class KafkaConfig {
	private static final Logger log = LoggerFactory.getLogger(KafkaFulfillmentListener.class);

	@Autowired
	ConfigProperties config;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean 
	public Map<String,Object> appKafkaConfig() throws Exception {
		Properties kafkaconfig = new Properties();
		try {
			kafkaconfig
					.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("app-kafka.properties"));
		} catch (Exception ex) {
			log.debug("app-kafka.properties not found.");
			throw ex;
		}
		kafkaconfig.setProperty("enable.auto.commit", "false");
		return new HashMap(kafkaconfig);
	}

	//--Fulfillment Message Consumer
    @Bean
    public ConsumerFactory<String, TraceableMessage<TransferFulfillmentMessage>> consumerFactory() throws Exception {

    	JacksonAvroDeserializer<TraceableMessage<TransferFulfillmentMessage>> jds = new JacksonAvroDeserializer<TraceableMessage<TransferFulfillmentMessage>>();
    	jds.configure(appKafkaConfig());
        return new DefaultKafkaConsumerFactory<>(appKafkaConfig(), new StringDeserializer(),  jds);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TraceableMessage<TransferFulfillmentMessage>> kafkaListenerContainerFactory() throws Exception {
    
        ConcurrentKafkaListenerContainerFactory<String, TraceableMessage<TransferFulfillmentMessage>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
    
    //--Dead Letter Queue Producer----------------------
	@Bean
	public ProducerFactory<String, String> producerFactory() throws Exception {
		Map<String,Object> kafkaprops = appKafkaConfig();
		return new DefaultKafkaProducerFactory<String, String>(kafkaprops);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() throws Exception {
		return new KafkaTemplate<>(producerFactory(), true); // auto-flush true, to force each message to broker.
	}
	
	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return mapper;
	}
	
	@Bean
	ElapsedTimeSLILogger kafkaTimer() {
		return new ElapsedTimeSLILogger(LoggerFactory.getLogger(KafkaDao.class), config.getAitid(), config.getKafkaTransferRequestQueue(), 
				Collections.singletonList(TransientDataAccessResourceException.class)); 
	}
}
