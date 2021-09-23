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

    public static SuspendedPatientStatus suspendedPatientStatus() {
        SuspendedPatientStatus suspendedPatientStatus = new SuspendedPatientStatus();
        suspendedPatientStatus.setIsSuspended(true);
        return suspendedPatientStatus;
    }

    public static SuspendedPatientStatus nonSuspendedPatientStatus(String odsCode) {
        SuspendedPatientStatus suspendedPatientStatus = new SuspendedPatientStatus();
        suspendedPatientStatus.setCurrentOdsCode(odsCode);
        suspendedPatientStatus.setIsSuspended(false);
        return suspendedPatientStatus;
    }
}
