package uk.nhs.prm.deductions.pdsadaptor.client.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Slf4j
public class OAuthRequestInterceptor implements ClientHttpRequestInterceptor {

    private final AuthService authService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("Intercepted request to pds fhir to add access token.");
        ClientHttpResponse response;
        String currentAccessToken = authService.getCurrentAccessToken();

        if (currentAccessToken.isEmpty()) {
            log.info("No access token available. Requesting new access token");
            addNewAuthTokenToRequest(request);
            response = execution.execute(request, body);
        } else {
            log.info("Access token currently exists adding token to request");
            request.getHeaders().add(AUTHORIZATION, "Bearer " + currentAccessToken);
            response = execution.execute(request, body);
            log.info("Got a target service response, and the response code: " + response.getStatusCode());
            if (HttpStatus.UNAUTHORIZED == response.getStatusCode()) {
                log.info("Request received 401 status. Requesting new access token");
                addNewAuthTokenToRequest(request);
                response = execution.execute(request, body);
            }
        }
        return response;

    }

    private void addNewAuthTokenToRequest(HttpRequest request) throws IOException {
        request.getHeaders().remove(AUTHORIZATION);
        request.getHeaders().add(AUTHORIZATION, "Bearer " + authService.getNewAccessToken());
    }
}
