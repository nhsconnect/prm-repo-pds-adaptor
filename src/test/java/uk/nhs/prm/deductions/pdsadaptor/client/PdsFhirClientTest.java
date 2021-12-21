package uk.nhs.prm.deductions.pdsadaptor.client;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.NotFoundException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.PdsFhirRequestException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.ServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.TooManyRequestsException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsResponse;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsSuspendedResponse;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private RestTemplate restTemplate;

    private static final String PDS_FHIR_ENDPOINT = "http://pds-fhir.com/";

    private PdsFhirClient pdsFhirClient;

    private static final String NHS_NUMBER = "123456789";

    private static final String MANAGING_ORGANISATION = "B9087";

    private static final String RECORD_E_TAG = "W/\"1\"";

    private static final String URL_PATH = PDS_FHIR_ENDPOINT + "Patient/" + NHS_NUMBER;

    @Captor
    private ArgumentCaptor<HttpEntity> requestCapture;

    @BeforeEach
    void setUp() {
        pdsFhirClient = new PdsFhirClient(restTemplate, PDS_FHIR_ENDPOINT);
    }

    @Nested
    @DisplayName("PDS FHIR GET Request")
    class PdsFhirGetRequest {
        @Test
        void shouldSetHeaderOnRequestForGet() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, RECORD_E_TAG);

            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, HttpStatus.OK));

            pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

            verify(restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), requestCapture.capture(), eq(PdsResponse.class));
            HttpEntity value = requestCapture.getValue();
            assertThat(value.getHeaders().get("X-Request-ID").get(0)).isNotNull();
        }

        @Test
        void shouldReturnPdsResponseWithEtagFromHeaders() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag(RECORD_E_TAG);

            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

            assertThat(expected).isEqualTo(pdsResponse);
            assertThat(expected.getETag()).isEqualTo(RECORD_E_TAG);

        }

        @Test
        void shouldSetHeaderOnRequestForGetRemoveGzipFromETag() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag("W/\"1--gzip\"");

            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

            assertThat(expected).isEqualTo(pdsResponse);
            assertThat(expected.getETag()).isEqualTo(RECORD_E_TAG);
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsResourceInvalid() {
            String invalidNhsNumber = "1234";
            when((restTemplate).exchange(eq(PDS_FHIR_ENDPOINT + "Patient/" + invalidNhsNumber), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(invalidNhsNumber));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 400. reason 400 error");
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsReturnsInternalServerError() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 500. reason 500 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowServiceUnavailableExceptionWhenPdsFhirIsUnavailable() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.GET), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            Exception exception = assertThrows(ServiceUnavailableException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(
                NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Unavailable");
        }
    }

    @Nested
    @DisplayName("PDS FHIR Patch Request")
    class PdsFhirPatchRequest {

        @Test
        void shouldSetHeaderOnRequestForPatch() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, null);

            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, HttpStatus.OK));

            pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG));

            verify(restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), requestCapture.capture(), eq(PdsResponse.class));
            HttpEntity value = requestCapture.getValue();
            assertThat(value.getHeaders().get("X-Request-ID").get(0)).isNotNull();
            assertThat(value.getHeaders().getContentType().toString()).isEqualTo("application/json-patch+json");
            assertThat(value.getHeaders().get("If-Match").get(0)).isEqualTo(RECORD_E_TAG);
        }

        @Test
        void shouldReturnPdsResponseWithEtagFromHeadersAfterSuccessfulUpdate() {
            String tagVersion =  "W/\"2\"";
            String managingOrganisation = "A1234";
            PdsResponse pdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, managingOrganisation, tagVersion);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag(tagVersion);

            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(managingOrganisation,  RECORD_E_TAG));

            verify(restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), requestCapture.capture(), eq(PdsResponse.class));
            PdsPatchRequest requestBody = (PdsPatchRequest) requestCapture.getValue().getBody();

            assertThat(requestBody).isNotNull();
            assertThat(requestBody.getOp()).isEqualTo("replace");
            assertThat(requestBody.getPath()).isEqualTo("/managingOrganization");
            assertThat(requestBody.getValue().getType()).isEqualTo("Organization");
            assertThat(requestBody.getValue().getIdentifier().getPath()).isEqualTo("https://fhir.nhs.uk/Id/ods-organization-code");
            assertThat(requestBody.getValue().getIdentifier().getValue()).isEqualTo(managingOrganisation);


            assertThat(expected).isEqualTo(pdsResponse);
            assertThat(expected.getETag()).isEqualTo(tagVersion);
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsResourceInvalid() {
            String invalidNhsNumber = "1234";
            when((restTemplate).exchange(eq(PDS_FHIR_ENDPOINT + "Patient/" + invalidNhsNumber), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                invalidNhsNumber, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 400. reason 400 error");
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsReturnsInternalServerError() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 500. reason 500 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowServiceUnavailableExceptionWhenPdsFhirIsUnavailable() {
            when((restTemplate).exchange(eq(URL_PATH), eq(HttpMethod.PATCH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            Exception exception = assertThrows(ServiceUnavailableException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Unavailable");
        }

    }


}
