package org.example.board_cafe_kiosk_2603.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * MariaDB 연결에 필요한 주요 Bean을 등록하는 설정 클래스입니다.
 *
 * <p>{@code spring.datasource.mariadb} 하위 설정값을 기반으로
 * MariaDB용 {@link DataSource}를 생성하고, SQL 실행을 위한 {@link JdbcTemplate},
 * 트랜잭션 관리를 위한 {@link PlatformTransactionManager}를 함께 등록합니다.</p>
 *
 * <p>프로젝트에서 MariaDB를 기본 데이터베이스로 사용하므로 각 Bean에
 * {@link Primary}를 지정하여 동일 타입 Bean이 여러 개 있을 때 우선 주입되도록 설정합니다.</p>
 */
@Configuration
public class MariaDBConfig {

    /**
     * MariaDB 연결 정보를 담는 {@link DataSourceProperties} Bean을 생성합니다.
     *
     * <p>{@code application.properties}의 {@code spring.datasource.mariadb.*}
     * 설정값을 바인딩하여 DB URL, 사용자명, 비밀번호, 드라이버 정보를 관리합니다.</p>
     *
     * @return MariaDB 연결 설정 정보
     */
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.mariadb")
    public DataSourceProperties getDataSourceProperties() {
        /* 데이터 소스 프로퍼티 빈 생성
           URL, 계정, 비밀번호 등을 저장 */
        return new DataSourceProperties();
    }

    /**
     * MariaDB 접속에 사용할 기본 {@link DataSource} Bean을 생성합니다.
     *
     * <p>{@link #getDataSourceProperties()}에 바인딩된 설정값을 이용해
     * 실제 데이터베이스 커넥션을 생성할 수 있는 DataSource를 초기화합니다.</p>
     *
     * @return MariaDB 전용 DataSource
     */
    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        /* 데이터베이스 연결 객체 생성 */
        return getDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    /**
     * MariaDB에 SQL을 실행하기 위한 {@link JdbcTemplate} Bean을 생성합니다.
     *
     * <p>반복적인 JDBC 코드 없이 쿼리 실행, 결과 매핑, 예외 변환 등을
     * Spring 방식으로 처리할 수 있도록 지원합니다.</p>
     *
     * @param mariaDataSource MariaDB 연결에 사용할 DataSource
     * @return MariaDB 전용 JdbcTemplate
     */
    @Primary
    @Bean(name = "mariaJdbcTemplate")
    public JdbcTemplate mariaJdbcTemplate(DataSource mariaDataSource) {
        /* SQL 실행을 쉽게 해주는 Spring JDBC 객체 생성 */
        return new JdbcTemplate(mariaDataSource);
    }

    /**
     * MariaDB 트랜잭션 처리를 담당하는 {@link PlatformTransactionManager} Bean을 생성합니다.
     *
     * <p>서비스 계층의 {@code @Transactional} 처리 시 MariaDB 커넥션의
     * commit, rollback을 관리합니다.</p>
     *
     * @param mariaDataSource 트랜잭션을 관리할 MariaDB DataSource
     * @return MariaDB 전용 트랜잭션 관리자
     */
    @Primary
    @Bean(name = "mariaTxManager")
    public PlatformTransactionManager mariaTxManager(DataSource mariaDataSource) {
        /* 트랜잭션 관리자 생성 */
        return new DataSourceTransactionManager(mariaDataSource);
    }
}
