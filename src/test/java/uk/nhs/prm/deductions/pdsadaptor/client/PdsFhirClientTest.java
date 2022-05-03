package uk.nhs.prm.deductions.pdsadaptor.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.UnknownContentTypeException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.*;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsResponse;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsSuspendedResponse;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private AuthenticatingHttpClient httpClient;

    @Mock
    private PdsFhirPatchRejectionInterpreter patchRejectionInterpreter;

    private static final String PDS_FHIR_ENDPOINT = "http://pds-fhir.com/";

    private PdsFhirClient pdsFhirClient;

    private static final String NHS_NUMBER = "123456789";

    private static final String MANAGING_ORGANISATION = "B9087";

    private static final String RECORD_E_TAG = "W/\"1\"";

    private static final String URL_PATH = PDS_FHIR_ENDPOINT + "Patient/" + NHS_NUMBER;


    @Captor
    private ArgumentCaptor<HttpHeaders> headersCaptor;

    @BeforeEach
    void setUp() {
        pdsFhirClient = new PdsFhirClient(httpClient, patchRejectionInterpreter, PDS_FHIR_ENDPOINT, 3);
    }

    @Nested
    @DisplayName("PDS FHIR GET Request")
    class PdsFhirGetRequest {
        @Test
        void shouldSetHeaderOnRequestForGet() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, RECORD_E_TAG);

            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenReturn(new ResponseEntity<>(pdsResponse, HttpStatus.OK));

            pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

            verify(httpClient).get(eq(URL_PATH), headersCaptor.capture(), eq(PdsResponse.class));
            assertThat(headersCaptor.getValue().get("X-Request-ID").get(0)).isNotNull();
        }

        @Test
        void shouldReturnPdsResponseWithEtagFromHeaders() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag(RECORD_E_TAG);

            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenReturn(
                    new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

            assertThat(expected).isEqualTo(pdsResponse);
            assertThat(expected.getETag()).isEqualTo(RECORD_E_TAG);

        }

        @Test
        void shouldThrowBadRequestExceptionWhenPdsResourceInvalid() {
            String invalidNhsNumber = "1234";
            when(httpClient.get(eq(PDS_FHIR_ENDPOINT + "Patient/" + invalidNhsNumber), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.BAD_REQUEST, "bad-request-error"));

            Exception exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(invalidNhsNumber));

            assertThat(exception.getMessage()).isEqualTo("Received 400 error from PDS FHIR: error: 400 bad-request-error");
        }

        @Test
        void shouldThrowAccessTokenRequestExceptionWhenPdsReturnsForbiddenError() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.FORBIDDEN, "error"));

            Exception exception = assertThrows(AccessTokenRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).isEqualTo("Access token request failed status code: 403. reason 403 error");
        }

        @Test
        void shouldThrowAccessTokenRequestExceptionWhenPdsReturnsUnauthorizedError() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "error"));

            Exception exception = assertThrows(AccessTokenRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).isEqualTo("Access token request failed status code: 401. reason 401 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found 404");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsFhirIsUnavailable() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(
                    NHS_NUMBER));

            assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 503. reason 503 error");
        }

        @Test
        void shouldThrowRuntimeExceptionWhen200ResponseButUnexpectedBodyFromPdsFhir() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(UnknownContentTypeException.class);
            Exception exception = assertThrows(RuntimeException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).contains("PDS FHIR returned unexpected response body when requesting PDS Record");
        }

        @Test
        void shouldThrowRuntimeExceptionWhenUnexpectedErrorFromPdsFhir() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(new ResourceAccessException("not-responding"));
            Exception exception = assertThrows(RuntimeException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(exception.getMessage()).contains("not-responding");
        }
    }


    @Nested
    @DisplayName("PDS FHIR Patch Request")
    class PdsFhirPatchRequest {

        @Captor
        private ArgumentCaptor<Object> patchCaptor;

        @Test
        void shouldSetHeaderOnRequestForPatch() {
            PdsResponse pdsResponse = buildPdsResponse(NHS_NUMBER, "A1234", LocalDate.now().minusYears(1), null, null);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenReturn(
                    new ResponseEntity<>(pdsResponse, HttpStatus.OK));

            pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG));

            verify(httpClient).patch(eq(URL_PATH), headersCaptor.capture(), patchCaptor.capture(), eq(PdsResponse.class));
            assertThat(headersCaptor.getValue().getFirst("X-Request-ID")).isNotNull();
            assertThat(headersCaptor.getValue().getContentType().toString()).isEqualTo("application/json-patch+json");
            assertThat(headersCaptor.getValue().getFirst("If-Match")).isEqualTo(RECORD_E_TAG);
        }

        @Test
        void shouldReturnPdsResponseWithEtagFromHeadersAfterSuccessfulUpdate() {
            String tagVersion = "W/\"2\"";
            String managingOrganisation = "A1234";
            PdsResponse pdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, managingOrganisation, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag(tagVersion);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenReturn(
                    new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(managingOrganisation, RECORD_E_TAG));

            verify(httpClient).patch(eq(URL_PATH), headersCaptor.capture(), patchCaptor.capture(), eq(PdsResponse.class));
            PdsPatchRequest requestBody = (PdsPatchRequest) patchCaptor.getValue();

            assertThat(requestBody).isNotNull();
            assertThat(requestBody.getPatches()).hasSize(1);

            PdsPatch pdsPatch = requestBody.getPatches().get(0);
            assertThat(pdsPatch.getOp()).isEqualTo("add");
            assertThat(pdsPatch.getPath()).isEqualTo("/managingOrganization");
            assertThat(pdsPatch.getValue().getType()).isEqualTo("Organization");
            assertThat(pdsPatch.getValue().getIdentifier().getSystem()).isEqualTo("https://fhir.nhs.uk/Id/ods-organization-code");
            assertThat(pdsPatch.getValue().getIdentifier().getValue()).isEqualTo(managingOrganisation);


            assertThat(expected).isEqualTo(pdsResponse);
            assertThat(expected.getETag()).isEqualTo(tagVersion);
        }

        @Test
        void shouldThrowExceptionWhenUpdateIsToSameManagingOrganisationAndTherebyRjectedAsMakingNoChanges() {
            var httpException = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

            when(patchRejectionInterpreter.isRejectionDueToNotMakingChanges(httpException)).thenReturn(true);

            when(httpClient.patch(any(), any(), any(), any())).thenThrow(httpException);

            var exception = assertThrows(PdsFhirPatchInvalidSpecifiesNoChangesException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage())
                    .isEqualTo("PDS FHIR request failed status code: 400. reason Provided patch made " +
                            "no changes to the resource");
        }

        @Test
        void shouldThrowBadRequestExceptionWhenItsA400BadRequestButNotANoChangesRejection() {
            var httpException = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");

            when(patchRejectionInterpreter.isRejectionDueToNotMakingChanges(httpException)).thenReturn(false);

            when(httpClient.patch(any(), any(), any(), any())).thenThrow(httpException);

            var exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage()).isEqualTo("Received 400 error from PDS FHIR: error: 400 Bad Request");
        }

        @Test
        void shouldThrowBadRequestExceptionWhenPdsResourceInvalid() {
            String invalidNhsNumber = "1234";
            when(httpClient.patch(eq(PDS_FHIR_ENDPOINT + "Patient/" + invalidNhsNumber), any(), any(),
                    eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

            Exception exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    invalidNhsNumber, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage()).isEqualTo("Received 400 error from PDS FHIR: error: 400 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found 404");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowPdsFhirRequestExceptionWhenPdsFhirIsUnavailable() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            var exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 503. reason 503 error");
        }

        @Test
        void shouldThrowPdsFhirRequestExceptionWhenPdsFhirIsUnavailableAfterRetry() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            assertThrows(PdsFhirRequestException.class, () ->
                    pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, new UpdateManagingOrganisationRequest("ODS123", "someTag")));

            verify(httpClient, times(3)).patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class));
        }

        @Test
        void shouldThrowPdsFhirRequestExceptionWhenPdsFhirIsUnavailableAfterRetryWithSameRequestId() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                    new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            assertThrows(PdsFhirRequestException.class, () ->
                    pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, new UpdateManagingOrganisationRequest("ODS123", "someTag")));

            verify(httpClient, times(3)).patch(eq(URL_PATH), headersCaptor.capture(), patchCaptor.capture(), eq(PdsResponse.class));

            String requestId = "";

            try {
                requestId = headersCaptor.getValue().get("X-Request-ID").get(0);
            }
            catch (NullPointerException e) {
                System.out.println("value or it's sub value is null");
            }

            for (HttpHeaders header : headersCaptor.getAllValues()) {
                assertTrue((header.get("X-Request-ID").get(0).equals(requestId)));
            }

        }

    }
}
