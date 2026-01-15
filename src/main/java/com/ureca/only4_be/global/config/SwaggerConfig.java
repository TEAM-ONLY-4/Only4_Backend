package com.ureca.only4_be.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        Info info = new Info()
                .version("v2.3.0")
                .title("DEPlog API")
                .description("DEPlog API 목록입니다.");

        return new OpenAPI()
                .info(info);
    }
}