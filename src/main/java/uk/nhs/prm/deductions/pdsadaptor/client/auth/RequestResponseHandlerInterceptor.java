package uk.nhs.prm.deductions.pdsadaptor.client.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.Exceptions.PdsFhirRequestException;

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Slf4j
public class RequestResponseHandlerInterceptor implements ClientHttpRequestInterceptor {

    private final AuthService authService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        log.info("Added current access token in request to pds fhir");

        request.getHeaders().add(AUTHORIZATION, "Bearer " + authService.getCurrentToken());
        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
            if (HttpStatus.UNAUTHORIZED == response.getStatusCode()) {

                log.info("Request received 401 status. Requesting new access token");
                request.getHeaders().remove(AUTHORIZATION);

                request.getHeaders().add(AUTHORIZATION, "Bearer " + authService.getAccessToken());
                log.info("generated new access token in request to pds fhir after Unauthorized");
                response = execution.execute(request, body);
            }
            if(HttpStatus.UNAUTHORIZED != response.getStatusCode() &&
                    HttpStatus.OK != response.getStatusCode()){
                log.info("Request To PDS FHIR Api Failed "+response.getStatusText());
            }
        } catch (HttpClientErrorException e) {
            log.info("Request To PDS FHIR Api Failed with an Exception");
            throw new PdsFhirRequestException(e);
        }
        return response;
    }


}
