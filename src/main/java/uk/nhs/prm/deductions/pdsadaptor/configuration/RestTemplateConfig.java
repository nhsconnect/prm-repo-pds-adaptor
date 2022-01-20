package uk.nhs.prm.deductions.pdsadaptor.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.AuthService;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.OAuthRequestInterceptor;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Bean
    public RestTemplate apacheBasedRestTemplate(HttpComponentsClientHttpRequestFactory apacheHttpClientRequestFactory) {
        return new RestTemplate(apacheHttpClientRequestFactory);
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory apacheHttpClientRequestFactory() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);
        return requestFactory;
    }
}
