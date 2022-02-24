package uk.nhs.prm.deductions.pdsadaptor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
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
    public static final String RECORD_E_TAG = "W/\"2\"";


    @Test
    void shouldCallPdsFhirClientAndReturnResponseForNonSuspendedPatient() {
        PdsResponse pdsResponse = TestData.buildPdsResponse(NHS_NUMBER, "B1234", LocalDate.now().minusYears(1), null, RECORD_E_TAG);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getManagingOrganisation()).isNull();
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
    }

    @Test
    void shouldCallPdsFhirClientAndReturnResponseForNonSuspendedPatientWithManagingOrganisationIfSet() {
        PdsResponse pdsResponse = TestData.buildPdsResponse(NHS_NUMBER, "B1234", LocalDate.now().minusYears(1), "A9876", RECORD_E_TAG);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);
        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getManagingOrganisation()).isEqualTo("A9876");
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
    }

    @Test
    void shouldReturnSuspendedPatientResponseWhenNoGpPractitionerField() {
        PdsResponse pdsResponse = TestData.buildPdsSuspendedResponse(NHS_NUMBER , "B1234", RECORD_E_TAG);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);

        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(true);
        assertThat(expected.getCurrentOdsCode()).isNull();
        assertThat(expected.getManagingOrganisation()).isEqualTo("B1234");
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
    }

    @Test
    void shouldReturnSuspendedPatientResponseWithDeceasedStatusTrueWhenPdsFhirReturnsDeceasedDateTimeField() {
        PdsResponse pdsResponse = TestData.buildPdsDeceasedResponse(NHS_NUMBER ,  RECORD_E_TAG);
        when(pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);

        SuspendedPatientStatus expected = pdsService.getPatientGpStatus(NHS_NUMBER);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isNull();
        assertThat(expected.getManagingOrganisation()).isNull();
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
        assertThat(expected.isDeceased).isEqualTo(true);
    }
    @Test
    void shouldCallPdsFhirClientWithUpdateAndReturnResponseForNonSuspendedPatientWithManagingOrganisationIfSet() {
        PdsResponse pdsResponse = TestData.buildPdsResponse(NHS_NUMBER, "B1234", LocalDate.now().minusYears(1), "A9876", RECORD_E_TAG);

        UpdateManagingOrganisationRequest updateRequest = new UpdateManagingOrganisationRequest("A1234", "W/\"1\"");

        when(pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, updateRequest)).thenReturn(pdsResponse);

        SuspendedPatientStatus expected = pdsService.updatePatientManagingOrganisation(NHS_NUMBER, updateRequest);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(false);
        assertThat(expected.getCurrentOdsCode()).isEqualTo("B1234");
        assertThat(expected.getManagingOrganisation()).isEqualTo("A9876");
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
    }

    @Test
    void shouldCallPdsFhirWithUpdateReturnSuspendedPatientResponseWhenNoGpPractitionerField() {
        PdsResponse pdsResponse = TestData.buildPdsSuspendedResponse(NHS_NUMBER , "B1234", RECORD_E_TAG);

        UpdateManagingOrganisationRequest updateRequest = new UpdateManagingOrganisationRequest("A1234", "W/\"1\"");

        when(pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, updateRequest)).thenReturn(pdsResponse);

        SuspendedPatientStatus expected = pdsService.updatePatientManagingOrganisation(NHS_NUMBER, updateRequest);

        assertThat(expected.getNhsNumber()).isEqualTo("1234567890");
        assertThat(expected.getIsSuspended()).isEqualTo(true);
        assertThat(expected.getCurrentOdsCode()).isNull();
        assertThat(expected.getManagingOrganisation()).isEqualTo("B1234");
        assertThat(expected.getRecordETag()).isEqualTo(RECORD_E_TAG);
    }

}
