package org.example.board_cafe_kiosk_2603.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    // toss 결제시 사용. 지우면 안됨
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
