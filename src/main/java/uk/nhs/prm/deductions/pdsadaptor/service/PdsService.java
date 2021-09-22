package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PdsService {

    private final PdsFhirClient pdsFhirClient;

    public SuspendedPatientStatus getPatientGpStatus(String nhsNumber) {
        SuspendedPatientStatus suspendedPatientStatus = new SuspendedPatientStatus();
        PdsResponse pdsResponse = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);

        boolean hasGeneralPractitioner = hasGeneralPractitioner(pdsResponse);

        if (hasGeneralPractitioner) {
            String odsCode = getOdsCode(pdsResponse);
            suspendedPatientStatus.setCurrentOdsCode(odsCode);
            suspendedPatientStatus.setIsSuspended(false);
        } else {
            suspendedPatientStatus.setIsSuspended(true);
        }

        return suspendedPatientStatus;
    }

    private String getOdsCode(PdsResponse pdsResponse) {
        GeneralPractitioner generalPractitioner = pdsResponse.getGeneralPractitioner().get(0);
        return generalPractitioner.getIdentifier().getValue();
    }

    private boolean hasGeneralPractitioner(PdsResponse pdsResponse) {
        return pdsResponse.getGeneralPractitioner() != null && pdsResponse.getGeneralPractitioner().size() != 0;
    }
}
