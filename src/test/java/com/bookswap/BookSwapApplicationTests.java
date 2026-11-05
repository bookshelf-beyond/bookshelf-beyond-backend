package com.bookswap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DisplayName("애플리케이션 컨텍스트")
class BookSwapApplicationTests {

	@Test
	@DisplayName("스프링 컨텍스트가 정상적으로 로딩된다")
	void contextLoads() {
	}

}
