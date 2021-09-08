package uk.nhs.prm.deductions.pdsadaptor.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.auth.AuthService;
import uk.nhs.prm.deductions.pdsadaptor.auth.SignedJWTGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    SignedJWTGenerator signedJWTGenerator ;

    @Mock
    RestTemplate restTemplate;

    @Test
    public void shouldReturnAccessTokenWithSignedJWT() throws Exception {
        Mockito.when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");
        AuthService authService = new AuthService(signedJWTGenerator,restTemplate,"https://token-endpoint");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion",  "Test");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> accessToken = authService.getAccessToken();
        Mockito.verify(restTemplate).postForEntity("https://token-endpoint",request,String.class);

    }

}
