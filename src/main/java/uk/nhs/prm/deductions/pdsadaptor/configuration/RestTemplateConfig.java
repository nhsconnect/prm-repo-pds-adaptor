package uk.nhs.prm.deductions.pdsadaptor.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.AuthService;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.OAuthRequestInterceptor;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate pdsFhirRestTemplate(AuthService authService) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(new OAuthRequestInterceptor(authService)));
        return restTemplate;
    }
}
