package uk.nhs.prm.deductions.pdsadaptor.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private RestTemplate restTemplate;

    private static final String pdsFhirEndpoint = "http://pds-fhir.com/";

    private PdsFhirClient pdsFhirClient;

    @BeforeEach
    void setUp() {
        pdsFhirClient = new PdsFhirClient(restTemplate, pdsFhirEndpoint);
    }

    @Test
    void shouldRequestPdsFhirEndpoint() {
        String nhsNumber = "1234";
        when((restTemplate).getForEntity(pdsFhirEndpoint + "Patient/1234", String.class)).thenReturn(
            new ResponseEntity<>("OK", HttpStatus.OK));

        String result = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);

        assertThat(result).isEqualTo("OK");
    }
}
