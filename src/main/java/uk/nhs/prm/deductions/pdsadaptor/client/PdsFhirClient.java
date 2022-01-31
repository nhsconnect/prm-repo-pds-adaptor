package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.UnknownContentTypeException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirRequestException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchIdentifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchValue;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.ExceptionFactory.*;

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
        log.info("Making GET request for pds record from pds fhir");
        try {
            ResponseEntity<PdsResponse> response = httpClient.get(pdsFhirEndpoint + path, createHeaders(), PdsResponse.class);
            log.info("Successfully requested pds record");
            return getPdsResponse(response);
        } catch (HttpClientErrorException e) {
            log.error("Received 4xx HTTP Error from PDS FHIR when requesting PDS Record");
            throw createClientException(e);
        } catch (HttpServerErrorException e) {
            log.warn("PDS FHIR Server error when requesting PDS Record");
            throw new PdsFhirRequestException(e);
        } catch (UnknownContentTypeException e) {
            log.error("PDS FHIR returned unexpected response body", e);
            throw new RuntimeException("PDS FHIR returned unexpected response body", e);
        } catch (Exception e) {
            log.warn("Unexpected Exception", e);
            throw new RuntimeException(e);
        }
    }

    public PdsResponse updateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        String path = "Patient/" + nhsNumber;
        log.info("Making PATCH request to update managing organisation from pds fhir");
        try {
            PdsPatchRequest patchRequest = createPatchRequest(updateRequest.getPreviousGp());
            HttpHeaders requestHeaders = createUpdateHeaders(updateRequest.getRecordETag());
            ResponseEntity<PdsResponse> response =
                httpClient.patch(pdsFhirEndpoint + path, requestHeaders, patchRequest, PdsResponse.class);
            log.info("Successfully updated managing organisation on pds record");
            return getPdsResponse(response);
        } catch (HttpClientErrorException e) {
            log.error("Received 4xx HTTP Error from PDS FHIR when updating PDS Record");
            throw createPatchException(e);
        } catch (HttpServerErrorException e) {
            log.warn("PDS FHIR Server error when updating PDS Record");
            throw new PdsFhirRequestException(e);
        } catch (UnknownContentTypeException e) {
            log.error("PDS FHIR returned unexpected response body", e);
            throw new RuntimeException("PDS FHIR returned unexpected response body", e);
        } catch (Exception e) {
            log.warn("Unexpected Exception", e);
            throw new RuntimeException(e);
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
            pdsResponse.setETag(eTag);
            return pdsResponse;
        }
        return null;
    }
}
