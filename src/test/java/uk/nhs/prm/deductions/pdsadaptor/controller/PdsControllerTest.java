package uk.nhs.prm.deductions.pdsadaptor.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(PdsController.class)
class PdsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdsService pdsService;

    @Test
    void shouldCallPdsServiceWithNhsNumberOnDemographicsRequest() throws Exception {
        String nhsNumber = "1234";

        mockMvc.perform(get("/patients/" + nhsNumber)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        verify(pdsService,times(1)).getPatientGpStatus("1234");
    }
}
