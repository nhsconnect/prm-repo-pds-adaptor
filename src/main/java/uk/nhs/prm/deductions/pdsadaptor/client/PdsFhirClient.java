package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@Slf4j
public class PdsFhirClient {

    private final RestTemplate pdsFhirRestTemplate;
    private final String pdsFhirEndpoint;

    public PdsFhirClient(RestTemplate pdsFhirRestTemplate, @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint) {
        this.pdsFhirRestTemplate = pdsFhirRestTemplate;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
    }

    public ResponseEntity requestPdsRecordByNhsNumber(String nhsNumber) {
        String path = "Patient/" + nhsNumber;

        log.info("Sending request to pds for patient");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", UUID.randomUUID().toString());

        ResponseEntity response = (ResponseEntity) pdsFhirRestTemplate.exchange(pdsFhirEndpoint + path, HttpMethod.GET,  new HttpEntity<>(headers),  String.class);
        log.info("Successful request pds record for patient");

        return response;
    }
}
