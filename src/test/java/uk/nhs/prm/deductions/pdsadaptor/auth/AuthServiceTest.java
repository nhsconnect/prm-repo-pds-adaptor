package uk.nhs.prm.deductions.pdsadaptor.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    SignedJWTGenerator signedJWTGenerator;

    @Mock
    RestTemplate restTemplate;

    @Test
    public void shouldRequestAccessTokenWithSignedJWT() throws Exception {

        String tokenResponse = "{\"access_token\": \"Sr5PGv19wTEHJdDr2wx2f7IGd0cw\",\n" +
                " \"expires_in\": \"599\",\n" +
                " \"token_type\": \"Bearer\"}";

        Mockito.when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");
        AuthService authService = new AuthService(signedJWTGenerator, restTemplate, "https://token-endpoint");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion", "Test");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        Mockito.when((restTemplate).postForEntity("https://token-endpoint", request, String.class)).thenReturn(new ResponseEntity<String>(tokenResponse, HttpStatus.OK));

        String accessToken = authService.getAccessToken();

        Mockito.verify(restTemplate).postForEntity("https://token-endpoint", request, String.class);
        assertThat(accessToken).isEqualTo("Sr5PGv19wTEHJdDr2wx2f7IGd0cw");

    }

    @Test
    public void shouldHandleFailureFromTokenAccessRequestEndPoint() throws Exception {

        String tokenResponse = "{\"error\": \"invalid_request\",\n" +
                " \"error_description\": \"Error response\",\n" +
                " \"message_id\": \"error\"}";

        Mockito.when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");
        AuthService authService = new AuthService(signedJWTGenerator, restTemplate, "https://token-endpoint");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion", "Test");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        Mockito.when((restTemplate).postForEntity("https://token-endpoint", request, String.class)).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        String accessTokenResponse = authService.getAccessToken();

        Mockito.verify(restTemplate).postForEntity("https://token-endpoint", request, String.class);
//        assertThat(accessTokenResponse).isEqualTo(null);

    }

}
