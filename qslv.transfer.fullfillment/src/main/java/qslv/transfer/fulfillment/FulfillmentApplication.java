package qslv.transfer.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class FulfillmentApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(FulfillmentApplication.class);
        application.run(args);
	}

}
