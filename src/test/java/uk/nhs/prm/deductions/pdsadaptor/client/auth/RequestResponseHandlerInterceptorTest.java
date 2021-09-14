package uk.nhs.prm.deductions.pdsadaptor.client.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class RequestResponseHandlerInterceptorTest {

    @Mock
    private AuthService authService;

    @Test
    public void shouldAddAnAccessTokenToRequestOnFirstRequest() throws Exception {
        when(authService.getCurrentAccessToken()).thenReturn("");
        when(authService.getNewAccessToken()).thenReturn("1234567890");
        Request request = new Request();
        new OAuthRequestInterceptor(authService).intercept(request, new byte[0], new RequestExecution(UNAUTHORIZED));
        HttpHeaders headers = request.getHeaders();
        assertThat(headers.get(AUTHORIZATION).get(0)).isEqualTo("Bearer 1234567890");
    }

    @Test
    public void shouldSetToNewAccessTokenWhenCurrentAccessTokenExpires() throws Exception {
        when(authService.getCurrentAccessToken()).thenReturn("0987654321");
        when(authService.getNewAccessToken()).thenReturn("1234567890");
        Request request = new Request();
        new OAuthRequestInterceptor(authService).intercept(request, new byte[0], new RequestExecution(UNAUTHORIZED));
        HttpHeaders headers = request.getHeaders();
        assertThat(headers.get(AUTHORIZATION).get(0)).isEqualTo("Bearer 1234567890");
    }

    @Test
    public void shouldNotCallAuthServiceIfResponseIsAuthorised() throws Exception {
        when(authService.getCurrentAccessToken()).thenReturn("0987654321");
        Request request = new Request();
        HttpHeaders headers = request.getHeaders();
        new OAuthRequestInterceptor(authService).intercept(request, new byte[0], new RequestExecution(HttpStatus.OK));

        assertThat(headers.get("Authorization").get(0)).isEqualTo("Bearer 0987654321");
        verifyNoMoreInteractions(authService);
    }

    private static class Request implements HttpRequest {

        private final HttpHeaders headers = new HttpHeaders();

        @Override
        public HttpMethod getMethod() {
            return null;
        }

        @Override
        public String getMethodValue() {
            return "";
        }

        @Override
        public URI getURI() {
            return URI.create("");
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }

    private static class RequestExecution implements ClientHttpRequestExecution {

        private final HttpStatus code;

        public RequestExecution(HttpStatus code) {
            this.code = code;
        }

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
            return new MockResponse(code);
        }
    }

    private static class MockResponse implements ClientHttpResponse {

        private final HttpStatus code;

        public MockResponse(HttpStatus code) {
            this.code = code;
        }

        @Override
        public HttpStatus getStatusCode() throws IOException {
            return code;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return 0;
        }

        @Override
        public String getStatusText() throws IOException {
            return "";
        }

        @Override
        public void close() {

        }

        @Override
        public InputStream getBody() throws IOException {
            return InputStream.nullInputStream();
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }

}
