package uk.nhs.prm.deductions.pdsadaptor.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {


    private final SignedJWTGenerator signedJWTGenerator;
    private RestTemplate restTemplate;
    private String accessTokenEndpoint;

    public AuthService (SignedJWTGenerator signedJWTGenerator,RestTemplate restTemplate, @Value("${accessTokenEndpoint}") String accessTokenEndpoint){
        this.signedJWTGenerator = signedJWTGenerator;
        this.restTemplate = restTemplate;
        this.accessTokenEndpoint = accessTokenEndpoint;
    }

    public ResponseEntity getAccessToken() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String token = signedJWTGenerator.createSignedJWT();
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion",  token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> accessToken = restTemplate.postForEntity( accessTokenEndpoint, request , String.class );

        return accessToken;
    }

}
