package uk.nhs.prm.deductions.pdsadaptor.model.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@ResponseStatus(value= HttpStatus.SERVICE_UNAVAILABLE)
public class PdsFhirRequestException extends RuntimeException {
    public PdsFhirRequestException(HttpStatusCodeException e) {
        super(String.format("PDS FHIR request failed status code: %s. reason %s", e.getStatusCode().value(), e.getMessage()));
        log.info("PDS FHIR request failed - status code: {}, error: {}", e.getStatusCode().value(), e.getMessage());
    }
}
