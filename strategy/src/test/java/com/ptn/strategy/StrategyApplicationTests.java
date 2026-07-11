package com.ptn.strategy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.ptn.strategy.news.task.CrawlTaskMapper;

@SpringBootTest
@ActiveProfiles("test")
class StrategyApplicationTests {

	@MockitoBean
	private CrawlTaskMapper crawlTaskMapper;

	@Test
	void contextLoads() {
	}

}
