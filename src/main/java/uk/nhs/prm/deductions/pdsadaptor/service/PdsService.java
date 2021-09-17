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
        GeneralPractitioner generalPractitioner = getGeneralPractitioner(pdsResponse);
        String odsCode = generalPractitioner.getIdentifier().getValue();
        LocalDate gpEndDate = generalPractitioner.getIdentifier().getPeriod().getEnd();
        if (gpEndDate != null && gpEndDate.isBefore(LocalDate.now())) {
            suspendedPatientStatus.setPreviousOdsCode(odsCode);
            suspendedPatientStatus.setIsSuspended(true);
        } else {
            suspendedPatientStatus.setCurrentOdsCode(odsCode);
            suspendedPatientStatus.setIsSuspended(false);
        }

        return suspendedPatientStatus;
    }

    private GeneralPractitioner getGeneralPractitioner(PdsResponse pdsResponse) {
        if (pdsResponse.getGeneralPractitioner() == null || pdsResponse.getGeneralPractitioner().size() == 0) {
            throw new NotFoundException("Unable to process request - GP Practice missing from response");
        }
        return pdsResponse.getGeneralPractitioner().get(0);
    }
}
