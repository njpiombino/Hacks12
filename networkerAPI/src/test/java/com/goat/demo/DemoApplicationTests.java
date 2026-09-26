package com.goat.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
		"spring.datasource.username=sa", "spring.datasource.password=" })
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
