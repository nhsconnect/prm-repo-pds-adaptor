package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsFhirPatient;

import java.util.UUID;

@Component
@Slf4j
public class RetryingPdsFhirClient {

    private final int maxUpdateTries;
    private final PdsFhirClient client;

    public RetryingPdsFhirClient(PdsFhirClient pdsFhirClient,
                                 @Value("${pds.fhir.update.number.of.tries}") int maxUpdateTries) {
        this.client = pdsFhirClient;
        this.maxUpdateTries = maxUpdateTries;
    }

    public PdsFhirPatient requestPdsRecordByNhsNumber(String nhsNumber) {
        return client.requestPdsRecordByNhsNumber(nhsNumber);
    }

    public PdsFhirPatient updateManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        return updateManagingOrganisationWithRetries(nhsNumber, updateRequest, UUID.randomUUID(), maxUpdateTries);
    }

    private PdsFhirPatient updateManagingOrganisationWithRetries(String nhsNumber, UpdateManagingOrganisationRequest updateRequest, UUID requestId, int triesLeft) {
        try {
            return client.updateManagingOrganisation(nhsNumber, updateRequest, requestId);
        }
        catch (PdsFhirServiceUnavailableException serverUnavailableException) {
            if (triesLeft > 1) {
                log.error("Retrying server update, tries remaining: " + (triesLeft - 1));
                return updateManagingOrganisationWithRetries(nhsNumber, updateRequest, requestId, triesLeft - 1);
            }
            log.error("Got server error after " + maxUpdateTries + " attempts.");
            throw serverUnavailableException;
        }
    }

}
