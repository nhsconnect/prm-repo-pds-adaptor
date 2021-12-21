package uk.nhs.prm.deductions.pdsadaptor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuspendedPatientStatus {
    private Boolean isSuspended;
    private String currentOdsCode;
    private String managingOrganisation;

    public static SuspendedPatientStatus suspendedPatientStatus(String managingOrganisation) {
        return SuspendedPatientStatus.builder()
            .isSuspended(true)
            .managingOrganisation(managingOrganisation)
            .build();
    }

    public static SuspendedPatientStatus nonSuspendedPatientStatus(String currentOdsCode, String managingOrganisation) {
        return SuspendedPatientStatus.builder()
            .isSuspended(false)
            .currentOdsCode(currentOdsCode)
            .managingOrganisation(managingOrganisation)
            .build();
    }
}
