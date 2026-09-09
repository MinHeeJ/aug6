package kr.ac.knue.commonfoundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
public class CommonFoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonFoundationApplication.class, args);
    }
}
