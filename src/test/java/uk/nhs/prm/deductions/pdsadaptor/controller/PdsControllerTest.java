package uk.nhs.prm.deductions.pdsadaptor.controller;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.AccessTokenRequestException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.BadRequestException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirServiceUnavailableException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.TooManyRequestsException;
import uk.nhs.prm.deductions.pdsadaptor.configuration.Tracer;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;
import uk.nhs.prm.deductions.pdsadaptor.testing.TestLogAppender;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.prm.deductions.pdsadaptor.testing.MapBuilder.json;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PdsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PdsControllerTest {

    private static final String NHS_NUMBER = "1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdsService pdsService;

    @MockBean
    private Tracer tracer;

    @Test
    void shouldCallPdsServiceWithNhsNumberOnDemographicsRequest() throws Exception {
        TestLogAppender testLogAppender = TestLogAppender.addTestLogAppender();
        ObjectMapper objectMapper = new ObjectMapper();

        SuspendedPatientStatus actualSuspendedPatientStatus = new SuspendedPatientStatus(NHS_NUMBER,true, null, null, "W1",false);
        when(pdsService.getPatientGpStatus(NHS_NUMBER)).thenReturn(actualSuspendedPatientStatus);
        doCallRealMethod().when(tracer).setTraceId("fake-trace-id");

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        String contentAsString = mockMvc.perform(get("/suspended-patient-status/" + NHS_NUMBER)
                .header("traceId", "fake-trace-id")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SuspendedPatientStatus suspendedPatientStatus = objectMapper.readValue(contentAsString, SuspendedPatientStatus.class);

        verify(pdsService,times(1)).getPatientGpStatus(NHS_NUMBER);
        assertThat(suspendedPatientStatus).isEqualTo(actualSuspendedPatientStatus);

        ILoggingEvent lastLoggedEvent = testLogAppender.getLastLoggedEvent();
        assertNotNull(lastLoggedEvent);
        assertTrue(lastLoggedEvent.getMDCPropertyMap().containsKey("traceId"));
        assertThat(lastLoggedEvent.getFormattedMessage()).isEqualTo("Request for pds record received from fake-user");
    }

    @Test
    void shouldCallPdsServiceWithNhsNumberAndUpdateRequest() throws Exception {
        TestLogAppender testLogAppender = TestLogAppender.addTestLogAppender();
        UpdateManagingOrganisationRequest updateRequest = new UpdateManagingOrganisationRequest("A1234", "W/\"2\"");
        ObjectMapper objectMapper = new ObjectMapper();

        SuspendedPatientStatus actualSuspendedPatientStatus = new SuspendedPatientStatus(NHS_NUMBER,true, null, "A1234", "W/\"3\"",false);
        when(pdsService.updatePatientManagingOrganisation(NHS_NUMBER, updateRequest)).thenReturn(actualSuspendedPatientStatus);
        doCallRealMethod().when(tracer).setTraceId("fake-trace-id");

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        String contentAsString = mockMvc.perform(put("/suspended-patient-status/" + NHS_NUMBER)
                .header("traceId", "fake-trace-id")
                .principal(mockPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .content(json(update -> update
                        .kv("previousGp", "A1234")
                        .kv("recordETag", "W/\"2\"")))
            )
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        verify(pdsService).updatePatientManagingOrganisation(NHS_NUMBER, updateRequest);
        SuspendedPatientStatus suspendedPatientStatus = objectMapper.readValue(contentAsString, SuspendedPatientStatus.class);

        assertThat(suspendedPatientStatus).isEqualTo(actualSuspendedPatientStatus);

        ILoggingEvent lastLoggedEvent = testLogAppender.getLastLoggedEvent();
        assertNotNull(lastLoggedEvent);
        assertTrue(lastLoggedEvent.getMDCPropertyMap().containsKey("traceId"));
        assertThat(lastLoggedEvent.getFormattedMessage()).isEqualTo("Update request for pds record received by fake-user");
    }

    @Test
    void shouldReturn400ResponseWhenPDSReturnsANonAuthNonRateLimit4xxResponse() throws Exception {
        when(pdsService.getPatientGpStatus(NHS_NUMBER)).thenThrow(BadRequestException.class);

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        mockMvc.perform(get("/suspended-patient-status/" + NHS_NUMBER)
                        .header("traceId", "fake-trace-id")
                        .principal(mockPrincipal))
                .andExpect(status().isBadRequest());

        verify(pdsService,times(1)).getPatientGpStatus(NHS_NUMBER);
    }

    @Test
    void shouldReturn503ResponseWhenPdsRequestReturnsAccessTokenRequestException() throws Exception {
        when(pdsService.getPatientGpStatus(NHS_NUMBER)).thenThrow(AccessTokenRequestException.class);

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        mockMvc.perform(get("/suspended-patient-status/" + NHS_NUMBER)
                        .header("traceId", "fake-trace-id")
                        .principal(mockPrincipal))
                .andExpect(status().isServiceUnavailable());

        verify(pdsService,times(1)).getPatientGpStatus(NHS_NUMBER);
    }

    @Test
    void shouldReturn503ResponseWhenPdsRequestReturnsTooManyRequestsException() throws Exception {
        when(pdsService.getPatientGpStatus(NHS_NUMBER)).thenThrow(TooManyRequestsException.class);

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        mockMvc.perform(get("/suspended-patient-status/" + NHS_NUMBER)
                        .header("traceId", "fake-trace-id")
                        .principal(mockPrincipal))
                .andExpect(status().isServiceUnavailable());

        verify(pdsService,times(1)).getPatientGpStatus(NHS_NUMBER);
    }

    @Test
    void shouldReturn503ResponseWhenPdsRequestReturnsAServerException() throws Exception {
        when(pdsService.getPatientGpStatus(NHS_NUMBER)).thenThrow(PdsFhirServiceUnavailableException.class);

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        mockMvc.perform(get("/suspended-patient-status/" + NHS_NUMBER)
                        .header("traceId", "fake-trace-id")
                        .principal(mockPrincipal))
                .andExpect(status().isServiceUnavailable());

        verify(pdsService,times(1)).getPatientGpStatus(NHS_NUMBER);
    }
}
