package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.SuspendedPatientStatus;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PdsService {

    private final PdsFhirClient pdsFhirClient;

    public SuspendedPatientStatus getPatientGpStatus(String nhsNumber) {
        SuspendedPatientStatus suspendedPatientStatus = new SuspendedPatientStatus();
        PdsResponse pdsResponse = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        GeneralPractitioner generalPractitioner = pdsResponse.getGeneralPractitioner().get(0);
        String odsCode = generalPractitioner.getIdentifier().getValue();
        LocalDate end = generalPractitioner.getIdentifier().getPeriod().getEnd();
        if (end != null && end.isBefore(LocalDate.now())) {
            suspendedPatientStatus.setPreviousOdsCode(odsCode);
            suspendedPatientStatus.setIsSuspended(true);
        } else {
            suspendedPatientStatus.setCurrentOdsCode(odsCode);
            suspendedPatientStatus.setIsSuspended(false);
        }
        return suspendedPatientStatus;
    }
}
