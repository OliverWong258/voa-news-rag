package com.ptn.strategy.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncConfiguration {

    @Bean(name = "ragStreamExecutor", destroyMethod = "close")
    public ExecutorService ragStreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
