package uk.nhs.prm.deductions.pdsadaptor.client.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
public class RequestResponseHandlerInterceptor implements ClientHttpRequestInterceptor {

    private final AuthService authService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (HttpStatus.UNAUTHORIZED == response.getStatusCode()) {
            request.getHeaders().remove(AUTHORIZATION);
            request.getHeaders().add(AUTHORIZATION, authService.getAccessToken());
            response = execution.execute(request, body);
        }
        return response;
    }

}
