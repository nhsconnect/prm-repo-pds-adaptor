package uk.nhs.prm.deductions.pdsadaptor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.auth.SignedJWTGenerator;

@Service
public class PDSFHIRClient {
    private SignedJWTGenerator signedJWTGenerator;
    @Value("${pdsFhirEndpoint}")
    String pdsFhirEndpoint;

    public PDSFHIRClient (SignedJWTGenerator signedJWTGenerator){
        this.signedJWTGenerator = signedJWTGenerator;
    }

//    public ResponseEntity getAccessToken() throws Exception {
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        String token = generateJWT.createSignedJWT();
//        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
//        map.add("grant_type", "client_credentials");
//        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
//        map.add("client_assertion",  token);
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
//
//        ResponseEntity<String> accessToken = restTemplate.postForEntity( pdsFhirEndpoint, request , String.class );
//
//        return accessToken;
//    }
}
