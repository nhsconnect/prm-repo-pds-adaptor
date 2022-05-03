package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.UnknownContentTypeException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.*;

import static java.lang.String.format;

@Slf4j
public class PdsFhirClientExceptionHandler {

    public static RuntimeException handleCommonExceptions(String description, Exception exception) {
        if (exception instanceof HttpClientErrorException) {
            log.error(format("Received 4xx HTTP Error from PDS FHIR when %s PDS Record", description));
            throw createClientException((HttpClientErrorException) exception);
        }

        if (exception instanceof UnknownContentTypeException) {
            log.error("PDS FHIR returned unexpected response body", exception);
            throw new RuntimeException(format("PDS FHIR returned unexpected response body when %s PDS Record", description), exception);
        }

        if (exception instanceof HttpServerErrorException) {
            log.warn(format("PDS FHIR Server error when %s PDS Record", description));
            throw new PdsFhirRequestException((HttpServerErrorException) exception);
        }

        log.warn("Unexpected Exception", exception);
        throw new RuntimeException(exception);
    }

    private static RuntimeException createClientException(HttpStatusCodeException exception) {
        if (exception.getStatusCode().equals(HttpStatus.FORBIDDEN) || exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            return new AccessTokenRequestException(exception);
        }
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new NotFoundException("PDS FHIR Request failed - Patient not found 404", exception);
        }
        if (exception.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
            return new TooManyRequestsException();
        }
        return new BadRequestException(exception);
    }
}
