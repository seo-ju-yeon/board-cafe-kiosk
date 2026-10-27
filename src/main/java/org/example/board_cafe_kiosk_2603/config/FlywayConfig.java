package org.example.board_cafe_kiosk_2603.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * Compose의 새 DB에 각자 독립된 마이그레이션과 이력 테이블을 적용한다.
 * JdbcTemplate, JobRepository와 벡터 저장소는 두 마이그레이션 완료 후 초기화된다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.database.migrations.enabled", havingValue = "true")
@Import(DatabaseInitializationDependencyConfigurer.class)
public class FlywayConfig {

    @Bean
    public Flyway mariaFlyway(@Qualifier("primaryDataSource") DataSource dataSource) {
        return configure(dataSource, "classpath:db/migration/mariadb").load();
    }

    @Bean
    public Flyway pgVectorFlyway(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return configure(dataSource, "classpath:db/migration/pgvector")
                .defaultSchema("public")
                .load();
    }

    @Bean
    public FlywayMigrationInitializer mariaFlywayInitializer(@Qualifier("mariaFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    @Bean
    public FlywayMigrationInitializer pgVectorFlywayInitializer(@Qualifier("pgVectorFlyway") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    private FluentConfiguration configure(DataSource dataSource, String location) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .encoding("UTF-8")
                .cleanDisabled(true)
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .validateMigrationNaming(true);
    }
}
