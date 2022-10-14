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
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsFhirGetPatientResponse;

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
        Optional<Name> nameOfTypeUsual = getNameOfTypeUsual(response);
        List<String> givenName = nameOfTypeUsual.map(Name::getGiven).orElse(null);
        String familyName = nameOfTypeUsual.map(Name::getFamily).orElse(null);
        String postalCode = getAddress(response).map(Address::getPostalCode).orElse(null);
        return new PatientTraceInformation(response.getId(), givenName, familyName, response.getBirthDate(), postalCode);
    }

    private boolean hasAddress(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getAddresses() != null && pdsResponse.getAddresses().size() != 0;
    }

    private Optional<Address> getAddress(PdsFhirGetPatientResponse response) {
        if (hasAddress(response)) {
            Optional<Address> addressOfTypeHome = response.getAddresses().stream().filter(
                    (address) -> Objects.equals(address.getUse(), "home") && address.getPeriod().isCurrent()
            ).findFirst();
            if (addressOfTypeHome.isEmpty()) {
                log.warn("PDS-FHIR response has no current 'address' of type 'home' for the patient");
            }
            return addressOfTypeHome;
        }
        log.warn("PDS-FHIR response does not include an address");
        return Optional.empty();
    }

    private boolean hasName(PdsFhirGetPatientResponse pdsResponse) {
        return pdsResponse.getName() != null && pdsResponse.getName().size() != 0;
    }
    private Optional<Name> getNameOfTypeUsual(PdsFhirGetPatientResponse pdsResponse) {
        if (hasName(pdsResponse)) {
            var nameOfTypeUsual = pdsResponse.getName().stream().filter(name ->
                    name.getUse().equalsIgnoreCase("Usual") && name.getPeriod().isCurrent()
            ).findFirst();
            if (nameOfTypeUsual.isEmpty()){
                log.warn("PDS-FHIR response has no current 'name' of type 'usual' for the patient");
            }
            return nameOfTypeUsual;
        }
        log.warn("PDS-FHIR response has no 'name' for the patient");
        return Optional.empty();
    }

}
