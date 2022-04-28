package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
public class PdsFhirClientExceptionFactory {

    public static RuntimeException createClientException(HttpStatusCodeException exception) {
        if (exception.getStatusCode().equals(HttpStatus.FORBIDDEN) || exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            return new AccessTokenRequestException(exception);
        }
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new NotFoundException("PDS FHIR Request failed - Patient not found");
        }
        if (exception.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
            return new TooManyRequestsException();
        }
        return new BadRequestException(exception);
    }

}
