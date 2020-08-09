package qslv.transfer.fulfillment;

import java.util.Map;
import javax.annotation.Resource;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import qslv.common.kafka.JacksonAvroDeserializer;
import qslv.common.kafka.TraceableMessage;
import qslv.transfer.request.TransferFulfillmentMessage;

@Configuration
public class KafkaListenerConfig {

	@Autowired
	ConfigProperties config;

	@Resource(name="listenerConfig")
	public Map<String,Object> listenerConfig;	

	//--Fulfillment Message Consumer
    @Bean
    public ConsumerFactory<String, TraceableMessage<TransferFulfillmentMessage>> consumerFactory() throws Exception {
    	
    	JacksonAvroDeserializer<TraceableMessage<TransferFulfillmentMessage>> jad = new JacksonAvroDeserializer<>();
    	jad.configure(listenerConfig);
    	
        return new DefaultKafkaConsumerFactory<>(listenerConfig, new StringDeserializer(),  jad);
    }
    
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, TraceableMessage<TransferFulfillmentMessage>>> kafkaListenerContainerFactory() throws Exception {
    
        ConcurrentKafkaListenerContainerFactory<String, TraceableMessage<TransferFulfillmentMessage>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
