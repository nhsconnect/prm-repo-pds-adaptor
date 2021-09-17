package uk.nhs.prm.deductions.pdsadaptor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.List;

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
        PdsResponse pdsResponse = new PdsResponse("1234", List.of());
        when(pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber)).thenReturn(pdsResponse);
        PdsResponse expected = pdsService.getPatientGpStatus(nhsNumber);
        assertThat(expected).isEqualTo(pdsResponse);
    }
}
