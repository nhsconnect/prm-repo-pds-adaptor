package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
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
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClientExceptionHandler.*;

@Component
@Slf4j
public class PdsFhirClient {

    private final String pdsFhirEndpoint;
    private final AuthenticatingHttpClient httpClient;
    private final int initialNumberOfPdsUpdateRetry;
    private final PdsFhirPatchRejectionInterpreter patchRejectionInterpreter;

    public PdsFhirClient(AuthenticatingHttpClient httpClient,
                         PdsFhirPatchRejectionInterpreter patchRejectionInterpreter,
                         @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint,
                         @Value("${pds.fhir.update.number.of.try}") int initialNumberOfTry) {
        this.httpClient = httpClient;
        this.patchRejectionInterpreter = patchRejectionInterpreter;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
        this.initialNumberOfPdsUpdateRetry = initialNumberOfTry;
    }

    public PdsResponse requestPdsRecordByNhsNumber(String nhsNumber) {
        log.info("Making GET request for pds record from pds fhir");
        return handleRequest("retrieval", () -> {
                var response = httpClient.get(patientUrl(nhsNumber), createRetrieveHeaders(), PdsResponse.class);
                log.info("Successfully requested pds record");
                return response;
            },
            exception -> {
                throw handleCommonExceptions("requesting", exception);
            });
    }

    public PdsResponse updateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        log.info("Making PATCH request to update managing organisation via pds fhir");

        var patchRequest = createPatchRequest(updateRequest.getPreviousGp());
        var requestHeaders = createUpdateHeaders(updateRequest.getRecordETag());

        return handleRequest("update",
                () -> {
                    var response = makePdsUpdateCall(patientUrl(nhsNumber), patchRequest, requestHeaders, initialNumberOfPdsUpdateRetry);
                    log.info("Successfully updated managing organisation on pds record.");
                    return response;
                },
                exception -> {
                    if (patchRejectionInterpreter.isRejectionDueToNotMakingChanges(exception)) {
                        log.error("Received 4xx HTTP Error from PDS FHIR when updating PDS Record");
                        throw new PdsFhirPatchInvalidSpecifiesNoChangesException();
                    }
                    throw handleCommonExceptions("updating", exception);
                });
    }

    private PdsResponse handleRequest(final String description,
                                      final Supplier<ResponseEntity<PdsResponse>> pdsRequestProcess,
                                      final Function<Exception, RuntimeException> exceptionHandler) {
        var startTime = Instant.now();
        try {
            return addEtagToResponseObject(pdsRequestProcess.get());
        }
        catch (Exception exception) {
            throw exceptionHandler.apply(exception);
        }
        finally {
            log.info("PDS-FHIR " + description + " took " + Duration.between(startTime, Instant.now()).toMillis() + "ms");
        }
    }

    private ResponseEntity<PdsResponse> makePdsUpdateCall(String url, PdsPatchRequest patchRequest, HttpHeaders requestHeaders, int triesLeft) {
        try {
            log.info("request id of the request: " + requestHeaders.get("X-Request-ID"));
            return httpClient.patch(url, requestHeaders, patchRequest, PdsResponse.class);
        }
        catch (HttpServerErrorException serverErrorException) {
            log.info("Got PDS-FHIR exception with status code : " + serverErrorException.getStatusCode());
            if (triesLeft > 1) {
                return makePdsUpdateCall(url, patchRequest, requestHeaders, triesLeft - 1);
            }
            log.error("Got server error after " + initialNumberOfPdsUpdateRetry + " attempts.");
            throw serverErrorException;
        }
    }

    private String patientUrl(String nhsNumber) {
        return pdsFhirEndpoint + "Patient/" + nhsNumber;
    }

    private HttpHeaders createRetrieveHeaders() {
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
}
