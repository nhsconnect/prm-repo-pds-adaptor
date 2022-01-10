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
        log.info("Added current access token in request to pds fhir");
        ClientHttpResponse response;
        try {
            String currentAccessToken = authService.getCurrentAccessToken();
            if(currentAccessToken.isEmpty()) {
                log.info("Current access token is empty");
            }
            request.getHeaders().add(AUTHORIZATION, "Bearer " + currentAccessToken );
            response = execution.execute(request, body);
            log.info("Got a target service response, and the response code: " + response.getStatusCode());

            if (HttpStatus.UNAUTHORIZED == response.getStatusCode()) {

                log.info("Request received 401 status. Requesting new access token");
                request.getHeaders().remove(AUTHORIZATION);
                request.getHeaders().add(AUTHORIZATION, "Bearer " + authService.getNewAccessToken());
                response = execution.execute(request, body);
            }
            return response;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
