package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;

@Slf4j
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class AccessTokenRequestException extends RuntimeException {
    public AccessTokenRequestException(HttpStatusCodeException e) {
        super(String.format("Access token request failed status code: %s. reason %s", e.getStatusCode().value(), e.getMessage()));
        log.info("Access token request failed - status code: {}, error: {}", e.getStatusCode().value(), e.getMessage());
    }

    public AccessTokenRequestException(IOException e) {
        super(String.format("Access token request failed: Cause: %s", e.getMessage()));
    }
}
