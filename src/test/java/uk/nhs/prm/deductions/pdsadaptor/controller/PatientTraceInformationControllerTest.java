package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.prm.deductions.pdsadaptor.model.PatientTraceInformation;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import java.security.Principal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PatientTraceInformationController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatientTraceInformationControllerTest {

    private static final String NHS_NUMBER = "1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdsService pdsService;

    @Test
    void shouldCallPdsServiceWithNhsNumberOnGetPatientDetailsRequest() throws Exception {
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("fake-user");

        PatientTraceInformation response = new PatientTraceInformation("0123456789",
                Arrays.asList("given name"),
                "family name", "some birthdate", "post code");
        when(pdsService.getPatientTraceInformation(NHS_NUMBER)).thenReturn(response);

        String contentAsString = mockMvc.perform(get("/patient-trace-information/" + NHS_NUMBER)
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var patientTraceInformation = new ObjectMapper().readValue(contentAsString, PatientTraceInformation.class);
        verify(pdsService).getPatientTraceInformation(NHS_NUMBER);
        assertEquals("0123456789",patientTraceInformation.getNhsNumber());
        assertEquals("given name",patientTraceInformation.getGivenName().get(0));
        assertEquals("family name",patientTraceInformation.getFamilyName());
        assertEquals("some birthdate",patientTraceInformation.getBirthdate());
        assertEquals("post code",patientTraceInformation.getPostalCode());

    }
}
