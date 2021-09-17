package uk.nhs.prm.deductions.pdsadaptor.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdsServiceTest {

    @Mock
    private PdsFhirClient pdsFhirClient;

    @InjectMocks
    private PdsService pdsService;

    @Test
    void shouldCallPdsFhirClientAndReturnResponseForNonSuspendedPatient() {
        String nhsNumber = "1234";
        PdsResponse pdsResponse = TestData.buildPdsResponse(nhsNumber, "B1234", LocalDate.now().minusYears(1), null);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(nhsNumber);

        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getPreviousOdsCode()).isNullOrEmpty();
    }

    @Test
    void shouldCallPdsFhirClientAndReturnResponseForSuspendedPatients() {
        String nhsNumber = "1234";
        PdsResponse pdsResponse = TestData.buildPdsResponse(nhsNumber, "B1234", LocalDate.now().minusYears(1), LocalDate.now().minusMonths(1));
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(nhsNumber);

        assertThat(expected.getIsSuspended()).isEqualTo(true);
        assertThat(expected.getCurrentOdsCode()).isNullOrEmpty();
        assertThat(expected.getPreviousOdsCode()).isEqualTo("B1234");
    }

    @Test
    void shouldCallPdsFhirClientAndReturnResponseForNonSuspendedPatientWithEndDate() {
        String nhsNumber = "1234";
        PdsResponse pdsResponse = TestData.buildPdsResponse(nhsNumber, "B1234", LocalDate.now().minusYears(1), LocalDate.now().plusMonths(1));
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(nhsNumber);

        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getPreviousOdsCode()).isNullOrEmpty();
    }

    @Test
    void shouldThrowAnErrorWhenNoGpPractitionerField() {
        String nhsNumber = "1234";
        PdsResponse pdsResponse = new PdsResponse(nhsNumber, null);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);

        Exception exception = assertThrows(NotFoundException.class, () -> pdsService.getPatientGpStatus(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Unable to process request - GP Practice missing from response");
    }

    @Test
    void shouldThrowAnErrorWhenEmptyGpPractitionerField() {
        String nhsNumber = "1234";
        PdsResponse pdsResponse = new PdsResponse(nhsNumber, List.of());
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);

        Exception exception = assertThrows(NotFoundException.class, () -> pdsService.getPatientGpStatus(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Unable to process request - GP Practice missing from response");
    }
}
