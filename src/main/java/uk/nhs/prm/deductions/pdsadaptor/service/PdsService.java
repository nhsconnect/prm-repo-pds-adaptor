package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.RetryingPdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Address;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.PatientTraceInformation;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Name;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsFhirGetPatientResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus.*;

@Service
@RequiredArgsConstructor
public class PdsService {

    private final RetryingPdsFhirClient pdsFhirClient;

    public SuspendedPatientStatus getPatientGpStatus(String nhsNumber) {
        PdsFhirGetPatientResponse pdsResponse = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        return convertToPatientStatusObject(pdsResponse);
    }

    public SuspendedPatientStatus updatePatientManagingOrganisation(String nhsNumber, UpdateManagingOrganisationRequest updateRequest) {
        PdsFhirGetPatientResponse pdsResponse = pdsFhirClient.updateManagingOrganisation(nhsNumber, updateRequest);
        return convertToPatientStatusObject(pdsResponse);
    }

    private SuspendedPatientStatus convertToPatientStatusObject(PdsFhirGetPatientResponse pdsResponse) {
        if (hasDeceasedDateAndTime(pdsResponse)) {
            return deceasedPatientStatus(pdsResponse.getId(), pdsResponse.getETag());
        }
        if (hasGeneralPractitioner(pdsResponse)) {
            return nonSuspendedPatientStatus(pdsResponse.getId(), getOdsCode(pdsResponse), getManagingOrganisation(pdsResponse), pdsResponse.getETag());
        }
        return suspendedPatientStatus(pdsResponse.getId(), getManagingOrganisation(pdsResponse), pdsResponse.getETag());
    }

    private String getOdsCode(PdsFhirGetPatientResponse pdsResponse) {
        GeneralPractitioner generalPractitioner = pdsResponse.getGeneralPractitioner().get(0);
        return generalPractitioner.getIdentifier().getValue();
    }

    private boolean hasGeneralPractitioner(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getGeneralPractitioner() != null && pdsResponse.getGeneralPractitioner().size() != 0;
    }


    private boolean hasDeceasedDateAndTime(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getDeceasedDateTime() != null;
    }

    private String getManagingOrganisation(PdsFhirGetPatientResponse pdsResponse) {
        if (pdsResponse.getManagingOrganization() != null) {
            return pdsResponse.getManagingOrganization().getIdentifier().getValue();
        }
        return null;
    }

    public PatientTraceInformation getPatientTraceInformation(String nhsNumber) {
        var response = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
        return convertToPatientTraceInformationObject(response);
    }

    private PatientTraceInformation convertToPatientTraceInformationObject(PdsFhirGetPatientResponse response) {
        return new PatientTraceInformation(response.getId(), getGivenName(response), response.getName().get(0).getFamily(), response.getBirthDate(), getPostalCode(response));
    }

    private boolean hasAddress(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getAddresses() != null && pdsResponse.getAddresses().size() != 0;
    }

    private String getPostalCode(PdsFhirGetPatientResponse response) {
        if (hasAddress(response)) {
            var homeAddress = response.getAddresses().stream().filter(
                    (address) -> Objects.equals(address.getUse(), "home") && address.getPeriod().isCurrent()
            ).findFirst();

            return homeAddress.map(Address::getPostalCode).orElse(null);
        }
        return null;
    }

    private boolean hasName(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getName() != null && pdsResponse.getName().size() != 0;
    }

    private String getFamilyName(PdsFhirGetPatientResponse response) {
        if (hasName(response)) {
            return response.getName().get(0).getFamily();
        }
        return null;
    }

    private boolean hasGivenName(Name name) {
        return name.getGiven() != null && name.getGiven().size() != 0;
    }

    private List<String> getGivenName(PdsFhirGetPatientResponse response) {
        if (hasName(response) && hasGivenName(response.getName().get(0))) {
            return response.getName().get(0).getGiven();
        }
        return null;
    }
}
