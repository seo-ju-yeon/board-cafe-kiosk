package org.example.board_cafe_kiosk_2603.config;

import org.example.board_cafe_kiosk_2603.ai.GameEmbeddingService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** scripts/verify-flyway.py가 만든 격리 DB만 사용하며 .env나 SpringBootTest를 로드하지 않는다. */
@Tag("flyway")
class FlywayMigrationTest {

    @Test
    void migrationsAreOptIn() {
        new ApplicationContextRunner().withUserConfiguration(FlywayConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(Flyway.class));
    }

    @Test
    void installsBothDatabasesBeforeConsumersAndPreservesDataOnRestart() throws Exception {
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        when(embedding.embed(anyString())).thenReturn(vector(true));
        when(embedding.embed(anyList(), any(), any())).thenAnswer(invocation -> {
            List<Document> documents = invocation.getArgument(0);
            return documents.stream().map(document -> vector(document.getText().contains("스플렌더"))).toList();
        });

        // 검증 URL은 검증 스크립트에서만 전달한다. 현재 앱의 .env로 폴백하지 않는다.
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class))
                .withUserConfiguration(MariaDBConfig.class, PgVectorConfig.class, FlywayConfig.class,
                        GameEmbeddingService.class)
                .withBean(EmbeddingModel.class, () -> embedding)
                .withPropertyValues(
                        "app.database.migrations.enabled=true", "spring.flyway.enabled=false",
                        "spring.batch.jdbc.initialize-schema=never", "spring.batch.job.enabled=false",
                        "spring.datasource.mariadb.url=" + required("FLYWAY_TEST_MARIADB_URL"),
                        "spring.datasource.mariadb.username=" + required("FLYWAY_TEST_USERNAME"),
                        "spring.datasource.mariadb.password=" + required("FLYWAY_TEST_PASSWORD"),
                        "spring.datasource.mariadb.driver-class-name=org.mariadb.jdbc.Driver",
                        "spring.datasource.pgvector.url=" + required("FLYWAY_TEST_PGVECTOR_URL"),
                        "spring.datasource.pgvector.username=" + required("FLYWAY_TEST_USERNAME"),
                        "spring.datasource.pgvector.password=" + required("FLYWAY_TEST_PASSWORD"),
                        "spring.datasource.pgvector.driver-class-name=org.postgresql.Driver");

        String validation = new ClassPathResource("sql/MariaDB/03_validate_reference_data.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String seedChecks = validation.substring(validation.indexOf("SELECT check_name"),
                validation.indexOf("-- GameEmbeddingService"));

        runner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate maria = context.getBean("mariaJdbcTemplate", JdbcTemplate.class);
            JdbcTemplate postgres = context.getBean("pgVectorJdbcTemplate", JdbcTemplate.class);
            assertHistory(maria, "1", "2");
            assertHistory(postgres, "1");
            var checks = maria.queryForList(seedChecks);
            assertThat(checks).hasSize(15);
            checks.forEach(row -> assertThat(row.get("result")).as(row.get("check_name").toString()).isEqualTo("PASS"));
            assertThat(postgres.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class)).isZero();
            verifyNoInteractions(embedding); // 마이그레이션과 빈 생성에는 임베딩 호출이 없다.

            // 실제 JobRepository로 MariaDB 전용 시퀀스와 6개 Batch 테이블을 검증한다.
            JobRepository repository = context.getBean(JobRepository.class);
            var execution = repository.createJobExecution("migrationProbe",
                    new JobParametersBuilder().addString("run", "first-install").toJobParameters());
            repository.add(execution.createStepExecution("probeStep"));
            assertThat(maria.queryForObject("SELECT COUNT(*) FROM BATCH_STEP_EXECUTION", Integer.class)).isEqualTo(1);

            // 고정된 가짜 임베딩으로 실제 저장소와 조회 코드의 호환성만 검사한다. 외부 API 호출 없음.
            assertThat(context.getBean(GameEmbeddingService.class).embedAllGames()).isEqualTo(6);
            var matches = context.getBean(VectorStore.class).similaritySearch(SearchRequest.builder()
                    .query("migration probe").topK(1).similarityThreshold(0.9).build());
            assertThat(matches).hasSize(1);
            assertThat(matches.getFirst().getMetadata()).containsEntry("gameName", "스플렌더");
            assertThat(postgres.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class)).isEqualTo(6);
            maria.update("UPDATE menu SET price = 3500 WHERE id = 1");

            for (String bean : List.of("mariaFlyway", "pgVectorFlyway")) {
                var configuration = context.getBean(bean, Flyway.class).getConfiguration();
                assertThat(configuration.isCleanDisabled()).isTrue();
                assertThat(configuration.isBaselineOnMigrate()).isFalse();
            }
        });

        clearInvocations(embedding);
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate maria = context.getBean("mariaJdbcTemplate", JdbcTemplate.class);
            JdbcTemplate postgres = context.getBean("pgVectorJdbcTemplate", JdbcTemplate.class);
            assertHistory(maria, "1", "2");
            assertHistory(postgres, "1");
            assertThat(maria.queryForObject("SELECT COUNT(*) FROM menu", Integer.class)).isEqualTo(22);
            assertThat(maria.queryForObject("SELECT price FROM menu WHERE id = 1", Integer.class)).isEqualTo(3500);
            assertThat(maria.queryForObject("SELECT COUNT(*) FROM BATCH_STEP_EXECUTION", Integer.class)).isEqualTo(1);
            assertThat(postgres.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class)).isEqualTo(6);
            verifyNoInteractions(embedding);
        });
    }

    private static void assertHistory(JdbcTemplate jdbc, String... versions) {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success = TRUE "
                + "AND version IS NOT NULL ORDER BY installed_rank", String.class)).containsExactly(versions);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Run python3 scripts/verify-flyway.py: missing " + name);
        }
        return value;
    }

    private static float[] vector(boolean splendor) {
        float[] value = new float[1536];
        value[splendor ? 1 : 0] = 1;
        return value;
    }
}
