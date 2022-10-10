package uk.nhs.prm.deductions.pdsadaptor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientTraceInformation {
    private String nhsNumber;
    private List<String> givenName;
    private String familyName;
    private String birthdate;
    private String postalCode;
}
