package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class Patient {
    private String nhsNumber;
    private List<GeneralPractitioner> generalPractitioner;
    private ManagingOrganization managingOrganization;
    private String eTag;
    private String deceasedDateTime;
    private String birthDate;
    private List<Address> addresses;
    private List<Name> name;

    public Optional<Name> getCurrentUsualName() {
        if (getName() == null || getName().size() == 0) {
            log.warn("PDS-FHIR response has no 'name' for the patient");
            return Optional.empty();
        }

        var nameOfTypeUsual = getName().stream().filter(name ->
                name.getUse().equalsIgnoreCase("Usual") && name.getPeriod().isCurrent()
        ).findFirst();

        if (nameOfTypeUsual.isEmpty()){
            log.warn("PDS-FHIR response has no current 'name' of type 'usual' for the patient");
        }
        return nameOfTypeUsual;
    }

    public Optional<Address> getCurrentHomeAddress() {
        var addresses = getAddresses();

        if (addresses == null) {
            return Optional.empty();
        }

        Optional<Address> addressOfTypeHome = addresses.stream().filter(
                (address) -> Objects.equals(address.getUse(), "home") && address.getPeriod().isCurrent()
        ).findFirst();
        if (addressOfTypeHome.isEmpty()) {
            log.warn("PDS-FHIR response has no current 'address' of type 'home' for the patient");
        }
        return addressOfTypeHome;
    }
}
