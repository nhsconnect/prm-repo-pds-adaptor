package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@ResponseStatus(value= HttpStatus.SERVICE_UNAVAILABLE) // how does this make any sense if this is a problem with the request and not the service?
                                                       // or is this request in the general sense as in we've got a general uncategorized failure while trying to talk to PDS FHIR?
                                                       // from PDS controller test this is used to support case of "server exception" so perhaps that's what this is meant to be?
public class PdsFhirRequestException extends RuntimeException {
    public PdsFhirRequestException(HttpStatusCodeException e) {
        super(String.format("PDS FHIR request failed status code: %s. reason %s", e.getStatusCode().value(), e.getMessage()));
        log.info("PDS FHIR request failed - status code: {}, error: {}", e.getStatusCode().value(), e.getMessage());
    }
}
