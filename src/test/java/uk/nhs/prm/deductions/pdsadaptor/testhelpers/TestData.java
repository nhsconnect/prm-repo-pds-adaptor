package uk.nhs.prm.deductions.pdsadaptor.testhelpers;

import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.GeneralPractitioner;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.Identifier;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.IdentifierPeriod;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.ManagingOrganization;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;
import java.util.List;

public class TestData {

    public static PdsResponse buildPdsResponse(String nhsNumber, String odsCode, LocalDate start,
                                               String managingOrganisationOdsCode, String eTag) {
        GeneralPractitioner generalPractitioner = createGeneralPractitioner(odsCode, start);
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsResponse(nhsNumber, List.of(generalPractitioner), managingOrganization, eTag,null);
    }

    public static PdsResponse buildPdsSuspendedResponse(String nhsNumber, String managingOrganisationOdsCode, String eTag) {
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsResponse(nhsNumber, null, managingOrganization, eTag,null);
    }
    public static PdsResponse buildPdsDeceasedResponse(String nhsNumber,  String eTag) {
        return new PdsResponse(nhsNumber,null, null, eTag,"\"2013-05-23T00:00:00+00:00\"");
    }

    private static GeneralPractitioner createGeneralPractitioner(String odsCode, LocalDate start) {
        IdentifierPeriod gpTimePeriod = new IdentifierPeriod(start, null);
        Identifier identifier = new Identifier(odsCode, gpTimePeriod);
        return new GeneralPractitioner(identifier);
    }

    private static ManagingOrganization createManagingOrganization(String managingOrganisationOdsCode) {
        Identifier identifier = new Identifier(managingOrganisationOdsCode, null);
        return new ManagingOrganization(identifier);
    }
}
