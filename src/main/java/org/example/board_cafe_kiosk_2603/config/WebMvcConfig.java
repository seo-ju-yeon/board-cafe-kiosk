package org.example.board_cafe_kiosk_2603.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // 업로드 파일을 정적 리소스로 서빙하기 위한 WebMvc 설정
    //   /upload/** 요청 → my.upload.path 디렉토리의 실제 파일로 매핑

    // 파일 삭제를 위해서 추가
    @Value("${my.upload.path}")
    private String uploadPath;  // 파일의 저장 경로

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
    }
}
