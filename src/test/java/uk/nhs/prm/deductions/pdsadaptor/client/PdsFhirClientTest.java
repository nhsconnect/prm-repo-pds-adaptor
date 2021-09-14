package uk.nhs.prm.deductions.pdsadaptor.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.Exceptions.PdsFhirRequestException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private RestTemplate restTemplate;

    private static final String pdsFhirEndpoint = "http://pds-fhir.com/";

    private PdsFhirClient pdsFhirClient;

    @Captor
    ArgumentCaptor<HttpEntity> requestCapture;

    @BeforeEach
    void setUp() {
        pdsFhirClient = new PdsFhirClient(restTemplate, pdsFhirEndpoint);
    }

    @Test
    void shouldRequestPdsFhirEndpoint() {
        String nhsNumber = "1234";
        when((restTemplate).exchange(eq(pdsFhirEndpoint + "Patient/1234"), eq(HttpMethod.GET), any(),  eq(String.class))).thenReturn(
            new ResponseEntity<>("OK", HttpStatus.OK));

        ResponseEntity result = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(restTemplate).exchange(eq(pdsFhirEndpoint + "Patient/1234"), eq(HttpMethod.GET), requestCapture.capture(), eq(String.class));
        HttpEntity value = requestCapture.getValue();
        assertThat(value.getHeaders().get("X-Request-ID").get(0)).isNotNull();
    }

    @Test
    void shouldThrowExceptionIfErrorRequestingPds() {
        String nhsNumber = "1234";
        when((restTemplate).exchange(eq(pdsFhirEndpoint + "Patient/1234"), eq(HttpMethod.GET), any(),  eq(String.class))).thenThrow(
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

        Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 400. reason 400 error");
    }
}
