package uk.nhs.prm.deductions.pdsadaptor.testhelpers;

import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GpIdentifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GpTimePeriod;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;
import java.util.List;

public class TestData {

    public static PdsResponse buildPdsResponse(String nhsNumber, String odsCode, LocalDate start, LocalDate end) {
        GpTimePeriod gpTimePeriod = new GpTimePeriod(start, end);
        GpIdentifier identifier = new GpIdentifier(odsCode, gpTimePeriod);
        GeneralPractitioner generalPractitioner = new GeneralPractitioner(identifier);
        return new PdsResponse(nhsNumber, List.of(generalPractitioner));
    }
}
