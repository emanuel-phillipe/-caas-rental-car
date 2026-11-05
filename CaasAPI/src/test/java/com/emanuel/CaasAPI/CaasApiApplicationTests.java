package com.emanuel.CaasAPI;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/caas_test",
        "spring.datasource.username=test",
        "spring.datasource.password=test"
})
class CaasApiApplicationTests {

	@Autowired
	HikariDataSource dataSource;

	@Test
	void contextLoadsWithConfiguredPostgresPool() {
		assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:1/caas_test");
		assertThat(dataSource.getDriverClassName()).isEqualTo("org.postgresql.Driver");
		assertThat(dataSource.getMaximumPoolSize()).isEqualTo(5);
		assertThat(dataSource.getDataSourceProperties().getProperty("sslmode")).isEqualTo("require");
	}

}
