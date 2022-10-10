package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdsFhirGetPatientResponse {
    private String id;
    private List<GeneralPractitioner> generalPractitioner;
    private ManagingOrganization managingOrganization;
    private String eTag;
    private String deceasedDateTime;
    private List<String> givenName;
    private String familyName;
    private String birthdate;
    private String postalCode;
}
