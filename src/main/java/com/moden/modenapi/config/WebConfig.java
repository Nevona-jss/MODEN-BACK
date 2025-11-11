package com.moden.modenapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                // DEV
                                "http://localhost:8080",
                                "http://127.0.0.1:3000",
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://192.168.1.25:3000",

                                // DEV over HTTPS (agar ishlatsangiz)
                                "https://localhost:3000",
                                "https://127.0.0.1:3000",
                                "https://localhost:5173",
                                "https://127.0.0.1:5173",

                                // PROD/Preview (o‘zingiznikini qo‘shing)
                                "https://moden-back.onrender.com"
                        )
                        .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }


    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> c : converters) {
            if (c instanceof MappingJackson2HttpMessageConverter jackson) {
                List<MediaType> types = new ArrayList<>(jackson.getSupportedMediaTypes());
                if (!types.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                    types.add(MediaType.APPLICATION_OCTET_STREAM); // ← treat octet-stream as JSON
                    jackson.setSupportedMediaTypes(types);
                }
            }
        }
    }
}

