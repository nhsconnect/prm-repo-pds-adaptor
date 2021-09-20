package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirRequestException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.ServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.TooManyRequestsException;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class PdsFhirClient {

    private final RestTemplate pdsFhirRestTemplate;
    private final String pdsFhirEndpoint;

    public PdsFhirClient(RestTemplate pdsFhirRestTemplate, @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint) {
        this.pdsFhirRestTemplate = pdsFhirRestTemplate;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
    }

    public PdsResponse requestPdsRecordByNhsNumber(String nhsNumber) {
        String path = "Patient/" + nhsNumber;
        log.info("Sending request to pds for patient");
        try {
            ResponseEntity<PdsResponse> response =
                pdsFhirRestTemplate.exchange(pdsFhirEndpoint + path, HttpMethod.GET, new HttpEntity<>(createHeaders()), PdsResponse.class);
            log.info("Successful request of pds record for patient");
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            handleExceptions(e);
            throw new PdsFhirRequestException(e);
        }
    }

    private void handleExceptions(HttpStatusCodeException e) {
        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            throw new NotFoundException("PDS FHIR Request failed - Patient not found");
        }
        if (e.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
            throw new TooManyRequestsException();
        }
        if (e.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            throw new ServiceUnavailableException();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
        return headers;
    }
}
