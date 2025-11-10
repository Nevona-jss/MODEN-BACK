package com.moden.modenapi;

import com.moden.modenapi.security.JwtProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;



@OpenAPIDefinition(
        info = @Info(title = "MODEN Hair Platform API", version = "1.0", description = "Backend API Documentation")
)
@EnableJpaAuditing
@EnableConfigurationProperties(JwtProperties.class)
@ConfigurationPropertiesScan
@SpringBootApplication
public class ModenApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModenApplication.class, args);
	}

}
