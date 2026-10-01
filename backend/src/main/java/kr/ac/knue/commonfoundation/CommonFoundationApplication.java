package kr.ac.knue.commonfoundation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots the shared Spring application and scans the BASIC-79 education module while preserving
 * the existing common-foundation package as the primary application boundary.
 */
@SpringBootApplication(scanBasePackages = {"kr.ac.knue.commonfoundation", "com.example.faculty"})
@MapperScan({"kr.ac.knue.commonfoundation", "com.example.faculty.achievement"})
public class CommonFoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonFoundationApplication.class, args);
    }
}
