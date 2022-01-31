package uk.nhs.prm.deductions.pdsadaptor.model.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@ResponseStatus(value= HttpStatus.BAD_GATEWAY)
public class BadGatewayException extends RuntimeException {
    public BadGatewayException(Exception exception) {
        super(String.format("PDS FHIR request failed - Reason: %s", exception.getMessage()));
        log.info("PDS FHIR request failed - Reason: {}", exception.getMessage());
    }
}