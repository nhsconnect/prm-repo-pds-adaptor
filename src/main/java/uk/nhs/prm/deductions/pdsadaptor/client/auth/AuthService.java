package uk.nhs.prm.deductions.pdsadaptor.client.auth;

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
import org.springframework.web.client.HttpStatusCodeException;
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

    public String getAccessToken() throws IOException {
        HttpEntity<MultiValueMap<String, String>> request = createRequestEntity();
        try {
            ResponseEntity<String> accessTokenResponse = restTemplate.postForEntity(accessTokenEndpoint, request, String.class);
            return getAccessTokenFromResponse(accessTokenResponse);
        } catch (HttpStatusCodeException e) {
            throw new AccessTokenRequestException(e);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createRequestEntity() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        requestBody.add("client_assertion", signedJWTGenerator.createSignedJWT());

        return new HttpEntity<>(requestBody, headers);
    }

    private String getAccessTokenFromResponse(ResponseEntity<String> accessTokenResponse) throws JsonProcessingException {
        JsonNode parent = new ObjectMapper().readTree(accessTokenResponse.getBody());
        return parent.get("access_token").asText();
    }

}
