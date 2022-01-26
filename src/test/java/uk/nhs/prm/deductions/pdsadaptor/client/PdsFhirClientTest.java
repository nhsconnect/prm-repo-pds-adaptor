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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import uk.nhs.prm.deductions.pdsadaptor.model.Exceptions.*;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;

import static java.nio.charset.StandardCharsets.UTF_8;
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
    private AuthenticatingHttpClient httpClient;

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
        pdsFhirClient = new PdsFhirClient(httpClient, PDS_FHIR_ENDPOINT);
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
                new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

            Exception exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(invalidNhsNumber));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Received status code: 400 BAD_REQUEST from PDS FHIR");
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsReturnsInternalServerError() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 500. reason 500 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowServiceUnavailableExceptionWhenPdsFhirIsUnavailable() {
            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            Exception exception = assertThrows(ServiceUnavailableException.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(
                NHS_NUMBER));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Unavailable");
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
            String tagVersion =  "W/\"2\"";
            String managingOrganisation = "A1234";
            PdsResponse pdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, managingOrganisation, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setETag(tagVersion);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenReturn(
                new ResponseEntity<>(pdsResponse, headers, HttpStatus.OK));

            PdsResponse expected = pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(managingOrganisation,  RECORD_E_TAG));

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
        void shouldThrowPdsFhirPatchExceptionWhenUpdateIsToSameManagingOrganisation() {
            String errorResponse = "{\n" +
                "    \"issue\": [\n" +
                "        {\n" +
                "            \"code\": \"structure\",\n" +
                "            \"details\": {\n" +
                "                \"coding\": [\n" +
                "                    {\n" +
                "                        \"code\": \"INVALID_UPDATE\",\n" +
                "                        \"display\": \"Update is invalid\",\n" +
                "                        \"system\": \"https://fhir.nhs.uk/R4/CodeSystem/Spine-ErrorOrWarningCode\",\n" +
                "                        \"version\": \"1\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"diagnostics\": \"Invalid update with error - Invalid patch - Provided patch made no changes to the resource\",\n" +
                "            \"severity\": \"error\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"resourceType\": \"OperationOutcome\"\n" +
                "}";

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenAnswer(
                (i) -> {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "400 Bad Request", errorResponse.getBytes(UTF_8), UTF_8);
                });

            Exception exception = assertThrows(PdsFhirPatchInvalidException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage())
                .isEqualTo("PDS FHIR request failed status code: 400. reason Provided patch made " +
                    "no changes to the resource");
        }

        @Test
        void shouldThrowBadRequestExceptionWhenNonePatchError() {
            String errorResponse = "{\n" +
                "    \"issue\": [\n" +
                "        {\n" +
                "            \"code\": \"structure\",\n" +
                "            \"details\": {\n" +
                "                \"coding\": [\n" +
                "                    {\n" +
                "                        \"code\": \"INVALID_UPDATE\",\n" +
                "                        \"display\": \"Update is invalid\",\n" +
                "                        \"system\": \"https://fhir.nhs.uk/R4/CodeSystem/Spine-ErrorOrWarningCode\",\n" +
                "                        \"version\": \"1\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"severity\": \"error\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"resourceType\": \"OperationOutcome\"\n" +
                "}";

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenAnswer(
                (i) -> {
                    throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", errorResponse.getBytes(UTF_8), UTF_8);
                });

            Exception exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Received status code: 400 BAD_REQUEST from PDS FHIR");

        }

        @Test
        void shouldThrowBadRequestExceptionWhenPdsResourceInvalid() {
            String invalidNhsNumber = "1234";
            when(httpClient.patch(eq(PDS_FHIR_ENDPOINT + "Patient/" + invalidNhsNumber), any(), any(),
                eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.BAD_REQUEST, "error"));

            Exception exception = assertThrows(BadRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                invalidNhsNumber, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Received status code: 400 BAD_REQUEST from PDS FHIR");
        }

        @Test
        void shouldThrowPdsFhirExceptionWhenPdsReturnsInternalServerError() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

            Exception exception = assertThrows(PdsFhirRequestException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR request failed status code: 500. reason 500 error");
        }

        @Test
        void shouldThrowNotFoundExceptionIfPatientNotFoundInPds() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.NOT_FOUND, "error"));

            Exception exception = assertThrows(NotFoundException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Request failed - Patient not found");
        }

        @Test
        void shouldThrowTooManyRequestsExceptionWhenExceedingPdsFhirRateLimit() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "error"));

            Exception exception = assertThrows(TooManyRequestsException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for PDS FHIR - too many requests");
        }

        @Test
        void shouldThrowServiceUnavailableExceptionWhenPdsFhirIsUnavailable() {
            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "error"));

            Exception exception = assertThrows(ServiceUnavailableException.class, () -> pdsFhirClient.updateManagingOrganisation(
                NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION,  RECORD_E_TAG)));

            Assertions.assertThat(exception.getMessage()).isEqualTo("PDS FHIR Unavailable");
        }

    }


}
