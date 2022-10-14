package uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PatientTest {
    @Test
    public void getCurrentUsualNameShouldGetCurrentUsualName() {
        var currentUsualName = new Name(new Period(
                LocalDate.now().minusYears(1),
                null), "usual", List.of("given name3"), "family name3");

        List<Name> names = new ArrayList<>();
        names.add(new Name(new Period(
                LocalDate.now().minusYears(1),
                null), "nickname", List.of("given name1"), "family name1"));
        names.add(new Name(new Period(
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(1)), "usual", List.of("given name2"), "family name2"));
        names.add(currentUsualName);

        Patient patient = new Patient("testId", null, null, null, null, null, null, names);

        var result = patient.getCurrentUsualName();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(currentUsualName);
    }

    @Test
    public void getCurrentUsualNameReturnsEmptyWhenPatientDoesNotHaveNames() {
        Patient patient = new Patient("testId", null, null, null, null, null, null, null);
        assertThat(patient.getCurrentUsualName().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentUsualNameReturnsEmptyWhenPatientHasEmptyNames() {
        Patient patient = new Patient("testId", null, null, null, null, null, null, List.of());
        assertThat(patient.getCurrentUsualName().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentUsualNameReturnsEmptyWhenPatientDoesNotHaveUsualName() {
        var currentNickName = new Name(new Period(
                LocalDate.now().minusYears(1),
                null), "nickname", List.of("given name3"), "family name3");

        Patient patient = new Patient("testId", null, null, null, null, null, null, List.of(currentNickName));
        assertThat(patient.getCurrentUsualName().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentUsualNameReturnsEmptyWhenPatientDoesNotHaveCurrentName() {
        var oldName = new Name(new Period(
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(1)), "usual", List.of("given name3"), "family name3");

        Patient patient = new Patient("testId", null, null, null, null, null, null, List.of(oldName));
        assertThat(patient.getCurrentUsualName().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentHomeAddressReturnsAMatchingAddress() {
        var currentHomeAddress = new Address(
                new Period(
                        LocalDate.now().minusYears(1),
                        null
                ),
                "POSTAL_CODE",
                "home"
        );

        Patient patient = new Patient("testId", null, null, null, null, null, List.of(currentHomeAddress), null);
        assertThat(patient.getCurrentHomeAddress().isPresent()).isTrue();
        assertThat(patient.getCurrentHomeAddress().get()).isEqualTo(currentHomeAddress);
    }

    @Test
    public void getCurrentHomeAddressReturnsEmptyWhenAddressesIsNull() {
        Patient patient = new Patient("testId", null, null, null, null, null, null, null);
        assertThat(patient.getCurrentHomeAddress().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentHomeAddressReturnsEmptyWhenAddressesIsEmpty() {
        Patient patient = new Patient("testId", null, null, null, null, null, List.of(), null);
        assertThat(patient.getCurrentHomeAddress().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentHomeAddressReturnsEmptyWhenNoHomeAddress() {
        var currentBillingAddress = new Address(
                new Period(
                        LocalDate.now().minusYears(1),
                        null
                ),
                "POSTAL_CODE",
                "billing"
        );

        Patient patient = new Patient("testId", null, null, null, null, null, List.of(currentBillingAddress), null);
        assertThat(patient.getCurrentHomeAddress().isEmpty()).isTrue();
    }

    @Test
    public void getCurrentHomeAddressReturnsEmptyWhenNoCurrentAddress() {
        var currentBillingAddress = new Address(
                new Period(
                        LocalDate.now().minusYears(2),
                        LocalDate.now().minusYears(1)
                ),
                "POSTAL_CODE",
                "home"
        );

        Patient patient = new Patient("testId", null, null, null, null, null, List.of(currentBillingAddress), null);
        assertThat(patient.getCurrentHomeAddress().isEmpty()).isTrue();
    }
}
