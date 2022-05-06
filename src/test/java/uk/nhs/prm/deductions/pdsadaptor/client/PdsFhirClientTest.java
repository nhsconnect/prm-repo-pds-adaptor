package uk.nhs.prm.deductions.pdsadaptor.client;

import org.jetbrains.annotations.NotNull;
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
import org.springframework.web.client.UnknownContentTypeException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.*;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsResponse;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsSuspendedResponse;

@ExtendWith(MockitoExtension.class)
class PdsFhirClientTest {

    @Mock
    private AuthenticatingHttpClient httpClient;

    @Mock
    private PdsFhirPatchRejectionInterpreter patchRejectionInterpreter;

    @Mock
    private PdsFhirExceptionHandler clientExceptionHandler;

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
        pdsFhirClient = new PdsFhirClient(httpClient, patchRejectionInterpreter, clientExceptionHandler, PDS_FHIR_ENDPOINT, 3);
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
        void shouldDelegateHandlingExceptionsToExceptionHandler() {
            var causingException = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
            var exceptionFromHandler = new RuntimeException("kabooey");

            when(httpClient.get(any(), any(), any())).thenThrow(causingException);
            when(clientExceptionHandler.handleCommonExceptions("requesting", causingException))
                    .thenThrow(exceptionFromHandler);

            var resultingException = assertThrows(Exception.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber("123"));

            assertThat(resultingException).isEqualTo(exceptionFromHandler);
        }

        @Test
        void shouldDelegateToExceptionHandlerWhenOkResponseButUnexpectedBodyFromPdsFhir() {
            var unknownResponseException = new UnknownContentTypeException(PdsResponse.class, APPLICATION_JSON, 200,
                    "ok", new HttpHeaders(), new byte[0]);
            var exceptionFromHandler = new RuntimeException("kabooey");

            when(httpClient.get(eq(URL_PATH), any(), eq(PdsResponse.class))).thenThrow(unknownResponseException);
            when(clientExceptionHandler.handleCommonExceptions("requesting", unknownResponseException))
                    .thenThrow(exceptionFromHandler);

            var resultingException = assertThrows(Exception.class, () -> pdsFhirClient.requestPdsRecordByNhsNumber(NHS_NUMBER));

            assertThat(resultingException).isEqualTo(exceptionFromHandler);
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
        void shouldReturnPdsResponseWithEtagFromHeadersAddedToItAfterSuccessfulUpdate() {
            String tagVersion = "W/\"2\"";
            String managingOrganisation = "A1234";
            var httpClientPdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, managingOrganisation, null);

            var headers = headersWithEtag(tagVersion);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class))).thenReturn(
                    new ResponseEntity<>(httpClientPdsResponse, headers, HttpStatus.OK));

            var updateResult = pdsFhirClient.updateManagingOrganisation(
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

            assertThat(updateResult).isEqualTo(httpClientPdsResponse);
            assertThat(updateResult.getETag()).isEqualTo(tagVersion);
        }

        @Test
        void shouldThrowExceptionWhenUpdateIsToSameManagingOrganisationAndTherebyRejectedAsMakingNoChanges() {
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
        void shouldDelegateToCommonExceptionHandlerWhenItsA400BadRequestButNotANoChangesRejection() {
            var causingException = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
            var exceptionFromHandler = new RuntimeException("kaboom");

            when(patchRejectionInterpreter.isRejectionDueToNotMakingChanges(causingException)).thenReturn(false);
            when(httpClient.patch(any(), any(), any(), any())).thenThrow(causingException);
            when(clientExceptionHandler.handleCommonExceptions("updating", causingException))
                    .thenThrow(exceptionFromHandler);

            var resultingException = assertThrows(Exception.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(resultingException).isEqualTo(exceptionFromHandler);
        }

        @Test
        void shouldDelegateHandlingAllOtherExceptionsToExceptionHandler() {
            var causingException = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
            var exceptionFromHandler = new RuntimeException("kabowza");

            when(httpClient.patch(any(), any(), any(), any())).thenThrow(causingException);
            when(clientExceptionHandler.handleCommonExceptions("updating", causingException))
                    .thenThrow(exceptionFromHandler);

            var resultingException = assertThrows(Exception.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest(MANAGING_ORGANISATION, RECORD_E_TAG)));

            assertThat(resultingException).isEqualTo(exceptionFromHandler);
        }

        @Test
        void shouldRetryIfPdsFhirServiceUnavailableWithSameRequestIdEachTime() {
            var initialException = new HttpServerErrorException(SERVICE_UNAVAILABLE, "error");

            when(clientExceptionHandler.handleCommonExceptions(any(), any()))
                    .thenThrow(PdsFhirGeneralServiceUnavailableException.class);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class)))
                    .thenThrow(initialException);

            assertThrows(Exception.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest("ODS123", "someTag")));

            verify(httpClient, times(3)).patch(eq(URL_PATH), headersCaptor.capture(), patchCaptor.capture(), eq(PdsResponse.class));

            var lastRequestIdUsed = headersCaptor.getValue().get("X-Request-ID").get(0);
            assertThat(requestIdsFromEachCall(headersCaptor)).allMatch(requestIdFromTry -> lastRequestIdUsed.equals(requestIdFromTry));
        }

        @Test
        void shouldRetryUpdateIfPdsUnavailableAndReturnSuccessfulResponseIfThenSuccessful() {
            var successfulPdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, "MOF12", null);

            when(clientExceptionHandler.handleCommonExceptions(any(), any()))
                    .thenThrow(PdsFhirGeneralServiceUnavailableException.class);

            when(httpClient.patch(eq(URL_PATH), any(), any(), eq(PdsResponse.class)))
                    .thenThrow(new RuntimeException())
                    .thenReturn(new ResponseEntity<>(successfulPdsResponse, headersWithEtag("\"new etag\""), HttpStatus.OK));

            var response = pdsFhirClient.updateManagingOrganisation(NHS_NUMBER, new UpdateManagingOrganisationRequest("ODS123", "previous etag"));

            assertThat(response).isEqualTo(successfulPdsResponse);
        }

        @Test
        void shouldDelegateToExceptionHandlerWhenPdsFhirIsUnavailableAfterRetry() {
            var exceptionFromHandler = new PdsFhirGeneralServiceUnavailableException(new HttpServerErrorException(SERVICE_UNAVAILABLE));
            var causalException = new RuntimeException("some http exception related to service unavailability");

            when(clientExceptionHandler.handleCommonExceptions("updating", causalException))
                    .thenThrow(exceptionFromHandler);

            when(httpClient.patch(any(), any(), any(), any())).thenThrow(causalException);

            var resultingException = assertThrows(Exception.class, () -> pdsFhirClient.updateManagingOrganisation(
                    NHS_NUMBER, new UpdateManagingOrganisationRequest("ODS123", "someTag")));

            verify(httpClient, times(3)).patch(any(), any(), any(), any());

            assertThat(resultingException).isEqualTo(exceptionFromHandler);
        }

        @NotNull
        private List<String> requestIdsFromEachCall(ArgumentCaptor<HttpHeaders> headersCaptor) {
            List<String> requestIdsFromEachTry = new ArrayList<>();
            for (HttpHeaders headers : headersCaptor.getAllValues()) {
                requestIdsFromEachTry.add(headers.get("X-Request-ID").get(0));
            }
            return requestIdsFromEachTry;
        }

        @NotNull
        private HttpHeaders headersWithEtag(String etag) {
            var headers = new HttpHeaders();
            headers.setETag(etag);
            return headers;
        }
    }
}
