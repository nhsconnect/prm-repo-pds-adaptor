package uk.nhs.prm.deductions.pdsadaptor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdsServiceTest {

    @Mock
    private PdsFhirClient pdsFhirClient;

    @InjectMocks
    private PdsService pdsService;
    public static final String NHS_NUMBER = "1234567890";

    @Test
    void shouldCallPdsFhirClientAndReturnResponseForNonSuspendedPatient() {
        PdsResponse pdsResponse = TestData.buildPdsResponse(NHS_NUMBER, "B1234", LocalDate.now().minusYears(1), null);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getPreviousOdsCode()).isNullOrEmpty();
    }

    @Test
    void shouldReturnSuspendedPatientResponseWhenNoGpPractitionerField() {
        PdsResponse pdsResponse = new PdsResponse(NHS_NUMBER, null);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);

        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getIsSuspended()).isEqualTo(true);
    }
}
