package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirPatchInvalidException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirRequestException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.ServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.TooManyRequestsException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchIdentifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchValue;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class PdsFhirClient {

    private final String pdsFhirEndpoint;
    private final AuthenticatingHttpClient httpClient;

    public PdsFhirClient(AuthenticatingHttpClient httpClient, @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint) {
        this.httpClient = httpClient;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
    }

    public PdsResponse requestPdsRecordByNhsNumber(String nhsNumber) {
        String path = "Patient/" + nhsNumber;
        log.info("Sending request to pds for patient");
        try {
            ResponseEntity<PdsResponse> response = httpClient.get(pdsFhirEndpoint + path, createHeaders(), PdsResponse.class);
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
            PdsPatchRequest patchRequest = createPatchRequest(updateRequest.getPreviousGp());
            HttpHeaders requestHeaders = createUpdateHeaders(updateRequest.getRecordETag());
            ResponseEntity<PdsResponse> response =
                httpClient.patch(pdsFhirEndpoint + path, requestHeaders, patchRequest, PdsResponse.class);
            log.info("Successful updated managing organisation on pds record");
            return getPdsResponse(response);
        } catch (HttpStatusCodeException e) {
            handlePatchInvalidException(e);
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

    private void handlePatchInvalidException(HttpStatusCodeException e) {
        if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            try {
                String diagnosticsValue = new JSONObject(e.getResponseBodyAsString(UTF_8))
                    .getJSONArray("issue")
                    .getJSONObject(0)
                    .getString("diagnostics");

                if (diagnosticsValue.contains("Provided patch made no changes to the resource")) {
                    throw new PdsFhirPatchInvalidException();
                }
            } catch (JSONException jsonException) {
                log.debug("Not invalid patch exception so ignoring");
            }
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
        PdsPatch patch = new PdsPatch("add", "/managingOrganization", patchValue);
        return new PdsPatchRequest(singletonList(patch));
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
