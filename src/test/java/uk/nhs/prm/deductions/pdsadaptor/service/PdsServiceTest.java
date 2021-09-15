package uk.nhs.prm.deductions.pdsadaptor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdsServiceTest {

    @Mock
    private PdsFhirClient pdsFhirClient;

    @InjectMocks
    private PdsService pdsService;

    @Test
    void shouldCallPdsFhirClientAndReturnResponse() {
        String nhsNumber = "1234";
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(new ResponseEntity(HttpStatus.OK));
        ResponseEntity patientGpStatus = pdsService.getPatientGpStatus(nhsNumber);
        assertThat(patientGpStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
