package uk.nhs.prm.deductions.pdsadaptor.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.prm.deductions.pdsadaptor.configuration.Tracer;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import java.security.Principal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PdsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PdsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdsService pdsService;

    @MockBean
    private Tracer tracer;

    @Test
    void shouldCallPdsServiceWithNhsNumberOnDemographicsRequest() throws Exception {
        TestLogAppender testLogAppender = addTestLogAppender();
        String nhsNumber = "1234567890";
        ObjectMapper objectMapper = new ObjectMapper();

        SuspendedPatientStatus actualSuspendedPatientStatus = new SuspendedPatientStatus(true, null, null, "W1");
        when(pdsService.getPatientGpStatus(nhsNumber)).thenReturn(actualSuspendedPatientStatus);
        doCallRealMethod().when(tracer).setTraceId("fake-trace-id");

        Principal mockPrincipal = Mockito.mock(Principal.class);
        Mockito.when(mockPrincipal.getName()).thenReturn("fake-user");

        String contentAsString = mockMvc.perform(get("/suspended-patient-status/" + nhsNumber)
                .header("traceId", "fake-trace-id")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SuspendedPatientStatus suspendedPatientStatus = objectMapper.readValue(contentAsString, SuspendedPatientStatus.class);

        verify(pdsService,times(1)).getPatientGpStatus(nhsNumber);
        assertThat(suspendedPatientStatus).isEqualTo(actualSuspendedPatientStatus);

        ILoggingEvent lastLoggedEvent = testLogAppender.getLastLoggedEvent();
        assertNotNull(lastLoggedEvent);
        assertTrue(lastLoggedEvent.getMDCPropertyMap().containsKey("traceId"));
        assertThat(lastLoggedEvent.getFormattedMessage()).isEqualTo("Request for pds record received by fake-user");
    }

    @NotNull
    private TestLogAppender addTestLogAppender() {
        TestLogAppender testLogAppender = new TestLogAppender();
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(testLogAppender);

        testLogAppender.start();
        return testLogAppender;
    }
}

class TestLogAppender extends AppenderBase<ILoggingEvent> {
    ArrayList<ILoggingEvent> loggingEvents = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        loggingEvents.add(eventObject);
    }

    ILoggingEvent getLastLoggedEvent() {
        if (loggingEvents.isEmpty()) return null;
        return loggingEvents.get(loggingEvents.size() - 1);
    }
}
