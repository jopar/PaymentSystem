package com.example.payment;

import com.example.payment.logging.MyLogger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource({"classpath*:applicationContext.xml"})
public class PaymentSystem {

	private final static MyLogger logger = new MyLogger(LoggerFactory.getLogger(PaymentSystem.class));

	public static void main(String[] args) {

//		ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

		logger.info("This is start of application");

		SpringApplication.run(PaymentSystem.class, args);
	}

}
