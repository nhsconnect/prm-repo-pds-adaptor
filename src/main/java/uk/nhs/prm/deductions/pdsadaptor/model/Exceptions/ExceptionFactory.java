package uk.nhs.prm.deductions.pdsadaptor.model.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class ExceptionFactory {
    public static RuntimeException createException(HttpStatusCodeException exception) {
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new NotFoundException("PDS FHIR Request failed - Patient not found");
        }
        if (exception.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
            return new TooManyRequestsException();
        }
        if (exception.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            return new ServiceUnavailableException();
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Received status code: " + exception.getStatusCode() + " from PDS FHIR");
        }
        return new PdsFhirRequestException(exception);
    }
}
