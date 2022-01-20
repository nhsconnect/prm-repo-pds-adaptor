package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class HttpClient {

    private final RestTemplate pdsFhirRestTemplate;


    public <T extends Object> ResponseEntity<T> makeGetRequest(String url, HttpHeaders headers, Class<T> responseType) {
        return pdsFhirRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    public <T extends Object> ResponseEntity<T> makePatchRequest(String url, HttpHeaders requestHeaders, Object patchRequest1,
                                                                 Class<T> responseType) {
        return pdsFhirRestTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(patchRequest1, requestHeaders), responseType);
    }
}
