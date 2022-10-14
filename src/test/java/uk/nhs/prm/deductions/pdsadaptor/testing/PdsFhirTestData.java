package uk.nhs.prm.deductions.pdsadaptor.testing;

import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PdsFhirTestData {

    public static Patient buildPdsResponse(String nhsNumber, String odsCode, LocalDate start,
                                           String managingOrganisationOdsCode, String eTag) {
        GeneralPractitioner generalPractitioner = createGeneralPractitioner(odsCode, start);
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new Patient(nhsNumber, List.of(generalPractitioner), managingOrganization, eTag, null, null, null, null);
    }

    public static Patient buildPdsSuspendedResponse(String nhsNumber, String managingOrganisationOdsCode, String eTag) {
        ManagingOrganization managingOrganization = createManagingOrganization(managingOrganisationOdsCode);
        return new Patient(nhsNumber, null, managingOrganization, eTag, null, null, null, null);
    }

    public static Patient buildPdsDeceasedResponse(String nhsNumber, String eTag) {
        return new Patient(nhsNumber, null, null, eTag, "\"2013-05-23T00:00:00+00:00\"", null, null, null);
    }

    public static Patient buildPatientDetailsResponse(String nhsNumber) {
        List<Name> names = new ArrayList<>();
        names.add(new Name(new Period(
                LocalDate.now().minusYears(1),
                null),"usual", List.of("given name"), "family name"));
        List<Address> address = new ArrayList<>();
        address.add(new Address(new Period(LocalDate.now().minusYears(1), null), "postal code", "home"));
        return new Patient(nhsNumber, null, null, null, null, "DateOfBirth", address, names);
    }

    public static Patient buildPatientDetailsResponseWithMultipleHomeAddresses(String nhsNumber) {
        List<Name> names = new ArrayList<>();
        names.add(new Name(new Period(
                LocalDate.now().minusYears(1),
                null),"usual",List.of("given name"), "family name"));
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address(new Period(LocalDate.now().minusYears(1), null), "temp postal code", "temp"));
        addresses.add(new Address(new Period(
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(1)
        ),"previous home postal code", "home"));
        addresses.add(new Address(new Period(
                LocalDate.now().minusYears(1),
                null
        ), "current home postal code", "home"));
        return new Patient(
                nhsNumber,
                null,
                null,
                null,
                null,
                "DateOfBirth",
                addresses,
                names
        );
    }

    private static GeneralPractitioner createGeneralPractitioner(String odsCode, LocalDate start) {
        Period gpTimePeriod = new Period(start, null);
        Identifier identifier = new Identifier(odsCode, gpTimePeriod);
        return new GeneralPractitioner(identifier);
    }

    private static ManagingOrganization createManagingOrganization(String managingOrganisationOdsCode) {
        Identifier identifier = new Identifier(managingOrganisationOdsCode, null);
        return new ManagingOrganization(identifier);
    }
}
