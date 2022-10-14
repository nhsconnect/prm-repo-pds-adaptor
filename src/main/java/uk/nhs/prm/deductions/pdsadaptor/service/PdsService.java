package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.RetryingPdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Address;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.PatientTraceInformation;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Name;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Patient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdsService {

    private final RetryingPdsFhirClient pdsFhirClient;

    public SuspendedPatientStatus getPatientGpStatus(String nhsNumber) {
        Patient pdsResponse = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        return convertToPatientStatusObject(pdsResponse);
    }

    public SuspendedPatientStatus updatePatientManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        Patient pdsResponse = pdsFhirClient.updateManagingOrganisation(nhsNumber, updateRequest);
        return convertToPatientStatusObject(pdsResponse);
    }

    private SuspendedPatientStatus convertToPatientStatusObject(Patient pdsResponse) {
        if (hasDeceasedDateAndTime(pdsResponse)) {
            return deceasedPatientStatus(pdsResponse.getNhsNumber(), pdsResponse.getETag());
        }
        if (hasGeneralPractitioner(pdsResponse)) {
            return nonSuspendedPatientStatus(pdsResponse.getNhsNumber(), getOdsCode(pdsResponse), getManagingOrganisation(pdsResponse), pdsResponse.getETag());
        }
        return suspendedPatientStatus(pdsResponse.getNhsNumber(), getManagingOrganisation(pdsResponse), pdsResponse.getETag());
    }

    private String getOdsCode(Patient pdsResponse) {
        GeneralPractitioner generalPractitioner = pdsResponse.getGeneralPractitioner().get(0);
        return generalPractitioner.getIdentifier().getValue();
    }

    private boolean hasGeneralPractitioner(Patient pdsResponse) {
        return pdsResponse.getGeneralPractitioner() != null && pdsResponse.getGeneralPractitioner().size() != 0;
    }


    private boolean hasDeceasedDateAndTime(Patient pdsResponse) {
        return pdsResponse.getDeceasedDateTime() != null;
    }

    private String getManagingOrganisation(Patient pdsResponse) {
        if (pdsResponse.getManagingOrganization() != null) {
            return pdsResponse.getManagingOrganization().getIdentifier().getValue();
        }
        return null;
    }

    public PatientTraceInformation getPatientTraceInformation(String nhsNumber) {
        var response = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        return convertToPatientTraceInformationObject(response);
    }

    private PatientTraceInformation convertToPatientTraceInformationObject(Patient patient) {
        Optional<Name> nameOfTypeUsual = patient.getCurrentUsualName();
        List<String> givenName = nameOfTypeUsual.map(Name::getGiven).orElse(null);
        String familyName = nameOfTypeUsual.map(Name::getFamily).orElse(null);
        String postalCode = patient.getCurrentHomeAddress().map(Address::getPostalCode).orElse(null);
        return new PatientTraceInformation(patient.getNhsNumber(), givenName, familyName, patient.getBirthDate(), postalCode);
    }
}
