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
import org.springframework.web.client.HttpServerErrorException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsSuspendedResponse;

@ExtendWith(MockitoExtension.class)
class RetryingPdsFhirClientTest {

    @Mock
    private PdsFhirClient oneShotClient;

    private RetryingPdsFhirClient retryingClient;

    private static final String NHS_NUMBER = "123456789";

    @Captor
    private ArgumentCaptor<UUID> requestIdCaptor;

    @BeforeEach
    void setUp() {
        retryingClient = new RetryingPdsFhirClient(oneShotClient, 3);
    }

    @Test
    void shouldGetPatientyWithoutRetry() {
        var pdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, "MOF12", null);

        when(oneShotClient.requestPdsRecordByNhsNumber(NHS_NUMBER)).thenReturn(pdsResponse);

        var actualResponse = retryingClient.requestPdsRecordByNhsNumber(NHS_NUMBER);

        assertThat(actualResponse).isEqualTo(pdsResponse);
    }

    @Test
    void shouldRetryUpdateIfPdsFhirServiceUnavailableWithSameRequestIdEachTime() {
        var initialException = new PdsFhirServiceUnavailableException(aServerException());
        var updateRequest = anUpdateRequest();

        when(oneShotClient.updateManagingOrganisation(any(), any(), any())).thenThrow(initialException);

        assertThrows(Exception.class, () -> {
            retryingClient.updateManagingOrganisation(NHS_NUMBER, updateRequest);
        });

        verify(oneShotClient, times(3)).updateManagingOrganisation(eq(NHS_NUMBER), eq(updateRequest), requestIdCaptor.capture());

        var lastRequestIdUsed = requestIdCaptor.getValue();
        assertThat(requestIdCaptor.getAllValues()).allMatch(requestIdFromTry -> lastRequestIdUsed.equals(requestIdFromTry));
    }

    @Test
    void shouldRetryUpdateIfPdsUnavailableAndReturnSuccessfulResponseIfThenSuccessful() {
        var successfulPdsResponse = buildPdsSuspendedResponse(NHS_NUMBER, "MOF12", null);

        when(oneShotClient.updateManagingOrganisation(any(), any(), any()))
                .thenThrow(new PdsFhirServiceUnavailableException(aServerException()))
                .thenReturn(successfulPdsResponse);

        var response = retryingClient.updateManagingOrganisation(NHS_NUMBER, anUpdateRequest());

        assertThat(response).isEqualTo(successfulPdsResponse);
    }

    @NotNull
    private HttpServerErrorException aServerException() {
        return new HttpServerErrorException(SERVICE_UNAVAILABLE, "error");
    }

    @NotNull
    private UpdateManagingOrganisationRequest anUpdateRequest() {
        return new UpdateManagingOrganisationRequest("ODS123", "someTag");
    }
}
