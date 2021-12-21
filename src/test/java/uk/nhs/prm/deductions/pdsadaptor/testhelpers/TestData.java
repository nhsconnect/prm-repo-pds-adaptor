package uk.nhs.prm.deductions.pdsadaptor.testhelpers;

import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Identifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.IdentifierPeriod;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.ManagingOrganization;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;
import java.util.List;

public class TestData {

    public static PdsResponse buildPdsResponse(String nhsNumber, String odsCode, LocalDate start, LocalDate end,
                                               String managingOrganisationOdsCode) {
        GeneralPractitioner generalPractitioner = createGeneralPractitioner(odsCode, start, end);
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsResponse(nhsNumber, List.of(generalPractitioner), managingOrganization);
    }

    public static PdsResponse buildPdsSuspendedResponse(String nhsNumber, String managingOrganisationOdsCode) {
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsResponse(nhsNumber, null, managingOrganization);
    }

    private static GeneralPractitioner createGeneralPractitioner(String odsCode, LocalDate start, LocalDate end) {
        IdentifierPeriod gpTimePeriod = new IdentifierPeriod(start, end);
        Identifier identifier = new Identifier(odsCode, gpTimePeriod);
        return new GeneralPractitioner(identifier);
    }

    private static ManagingOrganization createManagingOrganization(String managingOrganisationOdsCode) {
        Identifier identifier = new Identifier(managingOrganisationOdsCode, null);
        return new ManagingOrganization(identifier);
    }
}
