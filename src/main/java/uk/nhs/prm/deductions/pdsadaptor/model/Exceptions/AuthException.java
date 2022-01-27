package uk.nhs.prm.deductions.pdsadaptor.model.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@ResponseStatus(value= HttpStatus.SERVICE_UNAVAILABLE)
public class AuthException extends RuntimeException {
    public AuthException(HttpStatusCodeException e) {
        super("PDS FHIR request failed on auth - status code: " + e.getStatusCode().value());
        log.info("PDS FHIR request failed on auth - status code: " + e.getStatusCode().value());
    }
}