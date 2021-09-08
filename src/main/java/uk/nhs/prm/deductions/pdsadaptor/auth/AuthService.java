package uk.nhs.prm.deductions.pdsadaptor.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class AuthService {

    private final SignedJWTGenerator signedJWTGenerator;
    private final RestTemplate restTemplate;
    private final String accessTokenEndpoint;

    public AuthService(SignedJWTGenerator signedJWTGenerator, RestTemplate restTemplate, @Value("${accessTokenEndpoint}") String accessTokenEndpoint) {
        this.signedJWTGenerator = signedJWTGenerator;
        this.restTemplate = restTemplate;
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    public String getAccessToken() throws Exception {
        try {
            HttpEntity<MultiValueMap<String, String>> request = createRequestEntity();
            ResponseEntity<String> accessTokenResponse = restTemplate.postForEntity(accessTokenEndpoint, request, String.class);

            return getAccessToken(accessTokenResponse);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createRequestEntity() throws IOException, JOSEException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion", signedJWTGenerator.createSignedJWT());

        return new HttpEntity<>(map, headers);
    }

    private String getAccessToken(ResponseEntity<String> accessTokenResponse) throws JsonProcessingException {

        JsonNode parent = new ObjectMapper().readTree(accessTokenResponse.getBody());
        return parent.get("access_token").asText();
    }

}
