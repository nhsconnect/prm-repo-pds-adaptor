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
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsResponse;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private RestTemplate restTemplate;

    private static final String pdsFhirEndpoint = "http://pds-fhir.com/";

    private PdsFhirClient pdsFhirClient;

    private final String nhsNumber = "123456789";

    private final String urlPath = pdsFhirEndpoint + "Patient/123456789";

    @Captor
    private ArgumentCaptor<HttpEntity> requestCapture;

    @BeforeEach
    void setUp() {
        pdsFhirClient = new PdsFhirClient(restTemplate, pdsFhirEndpoint);
    }

    @Test
    void shouldSetHeaderOnRequest() {
        PdsResponse pdsResponse = buildPdsResponse(nhsNumber, "A1234", LocalDate.now().minusYears(1), null);

        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenReturn(
            new ResponseEntity<>(pdsResponse, HttpStatus.OK));

        pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);

        verify(restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), requestCapture.capture(), eq(PdsResponse.class));
        HttpEntity value = requestCapture.getValue();
        assertThat(value.getHeaders().get("X-Request-ID").get(0)).isNotNull();
    }

    @Test
    void shouldReturnPdsResponse() {
        PdsResponse pdsResponse = buildPdsResponse(nhsNumber, "A1234", LocalDate.now().minusYears(1), null);

        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenReturn(
            new ResponseEntity<>(pdsResponse, HttpStatus.OK));

        PdsResponse expected = pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);

        assertThat(expected).isEqualTo(pdsResponse);

    }

    @Test
    void shouldThrowExceptionIfErrorRequestingPds() {
        when(
            (restTemplate).exchange(eq(pdsFhirEndpoint + "Patient/123456789"), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

        Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 400. reason 400 error");
    }
}
