package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@ResponseStatus(value= HttpStatus.SERVICE_UNAVAILABLE) // how does this make any sense if this is a problem with the request and not the service?
                                                       // or is this request in the general sense as in we've got a general uncategorized failure while trying to talk to PDS FHIR?
                                                       // from PDS controller test this is used to support case of "server exception" so perhaps that's what this is meant to be?
                                                       //
                                                       // looking into history this exception was created originally within the RequestResponseInterceptor for adding auth and
                                                       // without a driving test - not a good start - and also:
                                                       // 1 - created only in case of HttpClientErrorException - so seemingly the Request part of name maybe did relate to the
                                                       //     client / request side of things being in error?
                                                       // 2 - was attributed with INTERNAL_SERVER_ERROR (for response from the pds-adaptor) - really denoting an irrecoverable
                                                       //     error (as we've managed to form an invalid client request to PDS FHIR?)...
                                                       // so looks like the meaning of this has just shifted over time within its ambiguity?
public class PdsFhirGeneralServiceUnavailableException extends RuntimeException {
    public PdsFhirGeneralServiceUnavailableException(HttpStatusCodeException e) {
        super(String.format("PDS FHIR request failed status code: %s. reason %s", e.getStatusCode().value(), e.getMessage()));
        log.info("PDS FHIR request failed - status code: {}, error: {}", e.getStatusCode().value(), e.getMessage());
    }
}
