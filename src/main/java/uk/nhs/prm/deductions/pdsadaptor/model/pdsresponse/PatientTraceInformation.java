package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class PatientTraceInformation {
    private String nhsNumber;
    private List<String> givenName;
    private String familyName;
    private String birthdate;
    private String postalCode;
}
