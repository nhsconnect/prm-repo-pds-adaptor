package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsFhirPatient;

import java.util.UUID;
import java.util.function.Supplier;

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
        var sharedRequestIdAcrossRetries = UUID.randomUUID();
        return processWithRetries(() -> client.updateManagingOrganisation(nhsNumber, updateRequest, sharedRequestIdAcrossRetries), maxUpdateTries
        );
    }

    private PdsFhirPatient processWithRetries(Supplier<PdsFhirPatient> updateProcess, int triesLeft) {
        try {
            return updateProcess.get();
        }
        catch (PdsFhirServiceUnavailableException serverUnavailableException) {
            if (triesLeft > 1) {
                log.error("Retrying server update, tries remaining: " + (triesLeft - 1));
                return processWithRetries(updateProcess, triesLeft - 1);
            }
            log.error("Got server error after " + maxUpdateTries + " attempts.");
            throw serverUnavailableException;
        }
    }

}
