package uk.nhs.prm.deductions.pdsadaptor.testing;

import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdsFhirTestData {

    public static PdsFhirGetPatientResponse buildPdsResponse(String nhsNumber, String odsCode, LocalDate start,
                                                             String managingOrganisationOdsCode, String eTag) {
        GeneralPractitioner generalPractitioner = createGeneralPractitioner(odsCode, start);
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsFhirGetPatientResponse(nhsNumber, List.of(generalPractitioner), managingOrganization, eTag, null, null, null, null);
    }

    public static PdsFhirGetPatientResponse buildPdsSuspendedResponse(String nhsNumber, String managingOrganisationOdsCode, String eTag) {
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new PdsFhirGetPatientResponse(nhsNumber, null, managingOrganization, eTag, null, null, null, null);
    }

    public static PdsFhirGetPatientResponse buildPdsDeceasedResponse(String nhsNumber, String eTag) {
        return new PdsFhirGetPatientResponse(nhsNumber, null, null, eTag, "\"2013-05-23T00:00:00+00:00\"", null, null, null);
    }

    public static PdsFhirGetPatientResponse buildPatientDetailsResponse(String nhsNumber) {
        List<Name> names = new ArrayList<>();
        names.add(new Name(Arrays.asList("bob"), "family name"));
        List<Address> address = new ArrayList<>();
        address.add(new Address("postal code"));
        return new PdsFhirGetPatientResponse(nhsNumber,null,null,null, null, "DateOfBirth", address, names);
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
