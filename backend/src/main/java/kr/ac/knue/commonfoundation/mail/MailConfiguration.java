package kr.ac.knue.commonfoundation.mail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VerificationMailProperties.class)
public class MailConfiguration {
}
