package com.example.payment;

import com.example.payment.adyen.controller.PaymentController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@SpringBootApplication
@ImportResource({"classpath*:applicationContext.xml"})
public class PaymentSystem {

	public static void main(String[] args) {

//		ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

		SpringApplication.run(PaymentSystem.class, args);
	}

}
