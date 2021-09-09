package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PdsFhirClient {

    private final RestTemplate restTemplate;
    private final String pdsFhirEndpoint;

    public PdsFhirClient(RestTemplate restTemplate, @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint) {
        this.restTemplate = restTemplate;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
    }

    public String requestPdsRecordByNhsNumber(String nhsNumber) {
        String path = "Patient/" + nhsNumber;
        restTemplate.getForEntity(pdsFhirEndpoint + path, String.class);
        log.info("Successfully request pds record for patient");
        return "OK";
    }
}
