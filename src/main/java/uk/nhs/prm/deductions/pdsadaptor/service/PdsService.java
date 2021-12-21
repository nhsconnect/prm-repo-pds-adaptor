package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import static uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus.*;

@Service
@RequiredArgsConstructor
public class PdsService {

    private final PdsFhirClient pdsFhirClient;

    public SuspendedPatientStatus getPatientGpStatus(String nhsNumber) {
        PdsResponse pdsResponse = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        return setPatientStatus(pdsResponse);
    }

    public SuspendedPatientStatus updatePatientManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        PdsResponse pdsResponse = pdsFhirClient.updateManagingOrganisation(nhsNumber, updateRequest);
        return setPatientStatus(pdsResponse);
    }

    private SuspendedPatientStatus setPatientStatus(PdsResponse pdsResponse) {
        if (hasGeneralPractitioner(pdsResponse)) {
            return nonSuspendedPatientStatus(getOdsCode(pdsResponse), getManagingOrganisation(pdsResponse), pdsResponse.getETag());
        }
        return suspendedPatientStatus(getManagingOrganisation(pdsResponse), pdsResponse.getETag());
    }

    private String getOdsCode(PdsResponse pdsResponse) {
        GeneralPractitioner generalPractitioner = pdsResponse.getGeneralPractitioner().get(0);
        return generalPractitioner.getIdentifier().getValue();
    }

    private boolean hasGeneralPractitioner(PdsResponse pdsResponse) {
        return pdsResponse.getGeneralPractitioner() != null && pdsResponse.getGeneralPractitioner().size() != 0;
    }

    private String getManagingOrganisation(PdsResponse pdsResponse) {
       if (pdsResponse.getManagingOrganization() != null) {
           return pdsResponse.getManagingOrganization().getIdentifier().getValue();
       }
       return null;
    }
}
