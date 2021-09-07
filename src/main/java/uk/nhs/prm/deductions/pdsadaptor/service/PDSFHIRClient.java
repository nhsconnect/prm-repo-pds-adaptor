package uk.nhs.prm.deductions.pdsadaptor.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.utlis.*;
@Service
public class PDSFHIRClient {


    private GenerateJWT generateJWT;

    public PDSFHIRClient (GenerateJWT generateJWT){
        this.generateJWT = generateJWT;
    }


    public ResponseEntity getAccessToken() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion",  generateJWT.createSignedJWT());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<String> accessToken = restTemplate.postForEntity( "https://int.api.service.nhs.uk/oauth2/token", request , String.class );

        return accessToken;
    }
}
