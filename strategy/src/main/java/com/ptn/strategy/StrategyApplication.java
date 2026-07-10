package com.ptn.strategy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@MapperScan("com.ptn.strategy.mapper")
@EnableScheduling
@ConfigurationPropertiesScan
public class StrategyApplication {

	public static void main(String[] args) {
		SpringApplication.run(StrategyApplication.class, args);
	}

	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
