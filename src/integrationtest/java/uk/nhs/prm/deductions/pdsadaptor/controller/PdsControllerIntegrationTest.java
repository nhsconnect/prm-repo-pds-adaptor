package uk.nhs.prm.deductions.pdsadaptor.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.deductions.pdsadaptor.client.auth.AuthService;

import java.io.IOException;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PdsControllerIntegrationTest {

    @MockBean
    AuthService authService;

    @Autowired
    TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;


    @Test
    public void shouldCallInterceptor() throws IOException {

        restTemplate.exchange(
                createURLWithPort("/patients/123"), HttpMethod.GET, null, HttpEntity.class);

        verify(authService, times(1)).getCurrentToken();
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }
}