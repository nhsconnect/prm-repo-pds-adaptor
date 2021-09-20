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
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirRequestException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.ServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.TooManyRequestsException;
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

    private static final String nhsNumber = "123456789";

    private static final String urlPath = pdsFhirEndpoint + "Patient/" + nhsNumber;

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
    void shouldThrowPdsFhirExceptionWhenPdsResourceInvalid() {
        String invalidNhsNumber = "1234";
        when((restTemplate).exchange(eq(pdsFhirEndpoint + "Patient/" + invalidNhsNumber), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
            new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

        Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(invalidNhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 400. reason 400 error");
    }

    @Test
    void shouldThrowPdsFhirExceptionWhenPdsReturnsInternalServerError() {
        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 500. reason 500 error");
    }

    @Test
    void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

        Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found");
    }

    @Test
    void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

        Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
    }

    @Test
    void shouldThrowServiceUnavailableExceptionWhenPdsFhirIsUnavailable() {
        when((restTemplate).exchange(eq(urlPath), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

        Exception exception = assertThrows(ServiceUnavailableException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber));

        Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Unavailable");
    }
}
