package com.tripai.backend;

import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // spring-dotenv는 spring.factories/AutoConfiguration.imports로 자동 등록되지 않아
        // ApplicationContextInitializer를 여기서 직접 등록해야 .env가 실제로 로드된다.
        new SpringApplicationBuilder(BackendApplication.class)
                .initializers(new DotenvApplicationInitializer())
                .run(args);
    }
}
