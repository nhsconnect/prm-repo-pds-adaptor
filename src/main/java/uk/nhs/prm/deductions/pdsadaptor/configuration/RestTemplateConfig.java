package uk.nhs.prm.deductions.pdsadaptor.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Bean
    public RestTemplate apacheBasedRestTemplate() {
        return new RestTemplate(apacheHttpClientRequestFactory());
    }

    private HttpComponentsClientHttpRequestFactory apacheHttpClientRequestFactory() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);
        return requestFactory;
    }
}
