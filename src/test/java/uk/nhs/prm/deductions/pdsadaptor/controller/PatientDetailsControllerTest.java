package uk.nhs.prm.deductions.pdsadaptor.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import java.security.Principal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PatientDetailsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PatientDetailsControllerTest {

    private static final String NHS_NUMBER = "1234567890";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdsService pdsService;

    @Test
    void shouldCallPdsServiceWithNhsNumberOnGetPatientDetailsRequest() throws Exception {
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("fake-user");

        mockMvc.perform(get("/patient-details/" + NHS_NUMBER)
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        verify(pdsService).getPatientDetails(NHS_NUMBER);
    }
}
