package uk.nhs.prm.deductions.pdsadaptor.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@ExtendWith(MockitoExtension.class)
class AuthenticatingHttpClientTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private AuthenticatingHttpClient authenticatingHttpClient;

    @Test
    void shouldAddAuthTokenToHeadersOnRequest() {
        String url = "url";
        String response = "someResponse";

        when(authService.getAccessToken()).thenReturn("authorised_token");
        when((httpClient).makeGetRequest(eq(url), any(), eq(String.class)))
            .thenAnswer(invocation -> {
                HttpHeaders headers = invocation.getArgument(1);
                assertThat(headers.get(AUTHORIZATION).get(0)).isEqualTo("Bearer authorised_token");
                return new ResponseEntity<>(response, HttpStatus.OK);
            });

        ResponseEntity<String> actualResponse = authenticatingHttpClient.makeGetRequest(url, new HttpHeaders(), String.class);

        assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualResponse.getBody()).isEqualTo(response);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRequestNewAccessTokenIfFirstRequestReturns401Status() {
        String url = "url";
        String response = "someResponse";

        when(authService.getAccessToken()).thenReturn("expired_token");
        when(authService.getNewAccessToken()).thenReturn("new_token");

        when((httpClient).makeGetRequest(eq(url), any(), eq(String.class)))
            .thenAnswer(invocation -> {
                HttpHeaders headers = invocation.getArgument(1);
                assertThat(headers.get(AUTHORIZATION).get(0)).isEqualTo("Bearer expired_token");
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
            })
            .thenAnswer(invocation -> {
                HttpHeaders headers = invocation.getArgument(1);
                assertThat(headers.get(AUTHORIZATION).get(0)).isEqualTo("Bearer new_token");
                return new ResponseEntity<>(response, HttpStatus.OK);
            });

        ResponseEntity<String> actual = authenticatingHttpClient.makeGetRequest(url, new HttpHeaders(), String.class);
        verify(httpClient, times(2)).makeGetRequest(eq(url), any(), eq(String.class));

        assertThat(actual.getBody()).isEqualTo(response);
    }

    @Test
    void shouldThrowHttpStatusExceptionIfNonUnauthorisedStatusError() {
        String url = "url";

        when(authService.getAccessToken()).thenReturn("authorised_token");

        when((httpClient).makeGetRequest(eq(url), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException( HttpStatus.BAD_REQUEST));

        assertThrows(HttpStatusCodeException.class, () -> authenticatingHttpClient.makeGetRequest(url, new HttpHeaders(), String.class));

        verify(httpClient, times(1)).makeGetRequest(eq(url), any(), eq(String.class));
        verifyNoMoreInteractions(authService);
    }
}
