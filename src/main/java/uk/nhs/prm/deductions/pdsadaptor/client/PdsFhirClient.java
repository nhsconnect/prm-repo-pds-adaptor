package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirGeneralServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirPatchInvalidSpecifiesNoChangesException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchIdentifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchValue;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j
public class PdsFhirClient {

    private final AuthenticatingHttpClient httpClient;
    private final PdsFhirPatchRejectionInterpreter patchRejectionInterpreter;
    private final PdsFhirExceptionHandler clientExceptionHandler;
    private final String pdsFhirEndpoint;
    private final int maxUpdateTries;

    public PdsFhirClient(AuthenticatingHttpClient httpClient,
                         PdsFhirPatchRejectionInterpreter patchRejectionInterpreter,
                         PdsFhirExceptionHandler clientExceptionHandler,
                         @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint,
                         @Value("${pds.fhir.update.number.of.tries}") int maxUpdateTries) {
        this.httpClient = httpClient;
        this.patchRejectionInterpreter = patchRejectionInterpreter;
        this.clientExceptionHandler = clientExceptionHandler;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
        this.maxUpdateTries = maxUpdateTries;
    }

    public PdsResponse requestPdsRecordByNhsNumber(String nhsNumber) {
        log.info("Making GET request for pds record from pds fhir");
        return timeRequest("retrieval", () -> {
            try {
                var response = httpClient.get(patientUrl(nhsNumber), createRetrieveHeaders(), PdsResponse.class);
                log.info("Successfully requested pds record");
                return addEtagToResponseObject(response);
            }
            catch (Exception exception) {
                throw clientExceptionHandler.handleCommonExceptions("requesting", exception);
            }
        });
    }

    public PdsResponse updateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        return doUpdateManagingOrganisationWithRetries(nhsNumber, updateRequest, UUID.randomUUID(), maxUpdateTries);
    }

    private PdsResponse doUpdateManagingOrganisationWithRetries(String nhsNumber, UpdateManagingOrganisationRequest updateRequest, UUID requestId, int triesLeft) {
        try {
            return doUpdateManagingOrganisation(nhsNumber, updateRequest, requestId);
        }
        catch (PdsFhirGeneralServiceUnavailableException serverUnavailableException) {
            if (triesLeft > 1) {
                log.error("Retrying server update, tries remaining: " + (triesLeft - 1));
                return doUpdateManagingOrganisationWithRetries(nhsNumber, updateRequest, requestId, triesLeft - 1);
            }
            log.error("Got server error after " + maxUpdateTries + " attempts.");
            throw serverUnavailableException;
        }
    }

    private PdsResponse doUpdateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest, UUID requestId) {
        log.info("Making PATCH request to update managing organisation via pds fhir");

        var patchRequest = createPatchRequest(updateRequest.getPreviousGp());
        var requestHeaders = createUpdateHeaders(updateRequest.getRecordETag(), requestId);

        return timeRequest("update", () -> {
            try {
                var response = httpClient.patch(patientUrl(nhsNumber), requestHeaders, patchRequest, PdsResponse.class);
                log.info("Successfully updated managing organisation on pds record.");
                return addEtagToResponseObject(response);
            }
            catch (Exception exception) {
                if (patchRejectionInterpreter.isRejectionDueToNotMakingChanges(exception)) {
                    log.error("Received 4xx HTTP Error from PDS FHIR when updating PDS Record");
                    throw new PdsFhirPatchInvalidSpecifiesNoChangesException();
                }
                throw clientExceptionHandler.handleCommonExceptions("updating", exception);
            }
        });
    }

    private HttpHeaders createRetrieveHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.toString());
        return headers;
    }

    private HttpHeaders createUpdateHeaders(String recordETag, UUID requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", requestId.toString());
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
        headers.setIfMatch(recordETag);
        return headers;
    }

    private PdsPatchRequest createPatchRequest(String managingOrganisation) {
        var identifier = new PdsPatchIdentifier("https://fhir.nhs.uk/Id/ods-organization-code", managingOrganisation);
        var patchValue = new PdsPatchValue("Organization", identifier);
        var patch = new PdsPatch("add", "/managingOrganization", patchValue);
        return new PdsPatchRequest(singletonList(patch));
    }

    private PdsResponse addEtagToResponseObject(ResponseEntity<PdsResponse> response) {
        PdsResponse pdsResponse = response.getBody();
        if (pdsResponse != null) {
            String eTag = response.getHeaders().getETag();
            pdsResponse.setETag(eTag);
            return pdsResponse;
        }
        return null;
    }

    private String patientUrl(String nhsNumber) {
        return pdsFhirEndpoint + "Patient/" + nhsNumber;
    }

    private PdsResponse timeRequest(final String description, final Supplier<PdsResponse> pdsRequestProcess) {
        var startTime = Instant.now();
        try {
            return pdsRequestProcess.get();
        }
        finally {
            log.info("PDS-FHIR " + description + " took " + Duration.between(startTime, Instant.now()).toMillis() + "ms");
        }
    }
}
