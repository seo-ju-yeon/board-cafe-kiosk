package org.example.board_cafe_kiosk_2603.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class PgVectorConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.pgvector")
    public DataSourceProperties pgVectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource() {
        return pgVectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }

    @Bean(name = "pgVectorTxManager")
    public PlatformTransactionManager pgVectorTxManager(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new DataSourceTransactionManager(pgVectorDataSource);
    }

    @Bean
    @DependsOnDatabaseInitialization
    public VectorStore vectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
                                   EmbeddingModel embeddingModel,
                                   @Value("${app.database.migrations.enabled:false}") boolean migrationsEnabled) {
        // 마이그레이션 설정이 켜지면 Flyway가 스키마를 관리하고 PgVectorStore가 벡터 테이블을 검증함.
        // 설정이 꺼지면 PgVectorStore가 스키마를 초기화함.
        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
                .dimensions(1536)  // OpenAI text-embedding-3-small 기준
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(!migrationsEnabled)
                .vectorTableValidationsEnabled(migrationsEnabled)
                .build();
    }
}
