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
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchIdentifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchValue;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.UUID;

import static java.util.Arrays.asList;
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
            return getPdsResponse(response);
        } catch (HttpStatusCodeException e) {
            handleExceptions(e);
            throw new PdsFhirRequestException(e);
        }
    }

    public PdsResponse updateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        String path = "Patient/" + nhsNumber;
        log.info("Sending patch request to pds for patient");
        try {
            PdsPatchRequest pdsPatchRequest = createPatchRequest(updateRequest.getPreviousGp());
            HttpHeaders requestHeaders = createUpdateHeaders(updateRequest.getRecordETag());
            ResponseEntity<PdsResponse> response =
                pdsFhirRestTemplate.exchange(pdsFhirEndpoint + path, HttpMethod.PATCH, new HttpEntity<>(pdsPatchRequest, requestHeaders), PdsResponse.class);
            log.info("Successful request of pds record for patient");
            return getPdsResponse(response);
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

    private HttpHeaders createUpdateHeaders(String recordETag) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
        headers.setIfMatch(recordETag);
        return headers;
    }

    private PdsPatchRequest createPatchRequest(String managingOrganisation) {
        PdsPatchIdentifier identifier =
            new PdsPatchIdentifier("https://fhir.nhs.uk/Id/ods-organization-code", managingOrganisation);
        PdsPatchValue patchValue = new PdsPatchValue("Organization", identifier);
        return new PdsPatchRequest("replace", "/managingOrganization", patchValue);
    }

    private PdsResponse getPdsResponse(ResponseEntity<PdsResponse> response) {
        PdsResponse pdsResponse = response.getBody();
        if (pdsResponse != null) {
            String eTag = response.getHeaders().getETag();
            pdsResponse.setETag(eTag != null ? eTag.replace("--gzip", "") : null);
            return pdsResponse;
        }
        return null;
    }
}
