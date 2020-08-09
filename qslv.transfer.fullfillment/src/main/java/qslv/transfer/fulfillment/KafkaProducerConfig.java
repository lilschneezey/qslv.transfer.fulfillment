package qslv.transfer.fulfillment;

import java.util.Map;
import javax.annotation.Resource;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class KafkaProducerConfig {
	@Autowired
	ConfigProperties config;

	@Resource(name="producerConfig")
	Map<String,Object> producerConfig;

	@Bean
	public ProducerFactory<String, String> transferDlqProducerFactory() throws Exception {
		return new DefaultKafkaProducerFactory<>(producerConfig, new StringSerializer(), new StringSerializer());
	}

	@Bean
	public KafkaTemplate<String, String> transferDlqKafkaTemplate() throws Exception {
		return new KafkaTemplate<>(transferDlqProducerFactory(), true); // auto-flush true, to force each message to broker.
	}
	
	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return mapper;
	}

}
