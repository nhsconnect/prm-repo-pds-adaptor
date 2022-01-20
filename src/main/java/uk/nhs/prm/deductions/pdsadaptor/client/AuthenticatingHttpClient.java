package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.AuthService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Component
public class AuthenticatingHttpClient {

    private final HttpClient httpClient;

    private final AuthService authService;

    public <T extends Object> ResponseEntity<T> makeGetRequest(String url, HttpHeaders headers, Class<T> responseType) {
        try {
            headers.add(AUTHORIZATION, "Bearer " + authService.getAccessToken());
            return httpClient.makeGetRequest(url, headers, responseType);
        }
        catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                headers.remove(AUTHORIZATION);
                headers.add(AUTHORIZATION, "Bearer " + authService.getNewAccessToken());
                return httpClient.makeGetRequest(url, headers, responseType);
            }
            throw e;
        }

    }

}
