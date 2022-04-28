package uk.nhs.prm.deductions.pdsadaptor.client.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private SignedJWTGenerator signedJWTGenerator;

    @Mock
    private RestTemplate restTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp()  {
        authService = new AuthService(signedJWTGenerator, restTemplate, "https://token-endpoint");
    }

    @Test
    public void shouldRequestAccessTokenWithSignedJWTWhenGettitngNewAccessToken() throws Exception {
        when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");

        String tokenResponse = "{\"access_token\": \"Sr5PGv19wTEHJdDr2wx2f7IGd0cw\",\n" +
            " \"expires_in\": \"599\",\n" +
            " \"token_type\": \"Bearer\"}";

        HttpEntity<MultiValueMap<String, String>> request = createRequest();

        when((restTemplate).postForEntity("https://token-endpoint", request, String.class)).thenReturn(
            new ResponseEntity<String>(tokenResponse, HttpStatus.OK));

        String accessToken = authService.getNewAccessToken();

        verify(restTemplate).postForEntity("https://token-endpoint", request, String.class);
        assertThat(accessToken).isEqualTo("Sr5PGv19wTEHJdDr2wx2f7IGd0cw");
        assertThat(authService.getAccessToken()).isEqualTo("Sr5PGv19wTEHJdDr2wx2f7IGd0cw");
    }

    @Test
    public void shouldRequestAccessTokenWithSignedJWTWhenCurrentAccessTokenIsEmpty() throws IOException {
        when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");

        String tokenResponse = "{\"access_token\": \"Sr5PGv19wTEHJdDr2wx2f7IGd0cw\",\n" +
            " \"expires_in\": \"599\",\n" +
            " \"token_type\": \"Bearer\"}";

        HttpEntity<MultiValueMap<String, String>> request = createRequest();

        when((restTemplate).postForEntity("https://token-endpoint", request, String.class)).thenReturn(
            new ResponseEntity<String>(tokenResponse, HttpStatus.OK));

        String accessToken = authService.getAccessToken();

        verify(restTemplate).postForEntity("https://token-endpoint", request, String.class);
        assertThat(accessToken).isEqualTo("Sr5PGv19wTEHJdDr2wx2f7IGd0cw");
        assertThat(authService.getAccessToken()).isEqualTo("Sr5PGv19wTEHJdDr2wx2f7IGd0cw");
    }

    @Test
    public void shouldReturnCurrentAccessTokenWhenAlreadySet() {
        authService.setAccessToken("someToken");
        String accessToken = authService.getAccessToken();
        verifyNoInteractions(signedJWTGenerator);
        verifyNoInteractions(restTemplate);
        assertThat(accessToken).isEqualTo("someToken");
    }

    @Test
    public void shouldHandleFailureFromTokenAccessRequestEndPoint() throws IOException {
        when(signedJWTGenerator.createSignedJWT()).thenReturn("Test");

        HttpEntity<MultiValueMap<String, String>> request = createRequest();

        when((restTemplate).postForEntity("https://token-endpoint", request, String.class)).thenThrow(
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

        assertThrows(HttpClientErrorException.class, authService::getNewAccessToken);

        verify(restTemplate).postForEntity("https://token-endpoint", request, String.class);
    }

    private HttpEntity<MultiValueMap<String, String>> createRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        map.add("client_assertion", "Test");

        return new HttpEntity<>(map, headers);
    }
}
