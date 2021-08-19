package uk.nhs.prm.deductions.pdsadaptor.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "PDS Adaptor", version = "v1"))
@SecurityScheme(name = "apiKey", type = SecuritySchemeType.APIKEY)
public class SwaggerConfig {
}
