package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PdsFhirClient {

    private final RestTemplate pdsFhirRestTemplate;
    private final String pdsFhirEndpoint;

    public PdsFhirClient(RestTemplate pdsFhirRestTemplate, @Value("${pdsFhirEndpoint}") String pdsFhirEndpoint) {
        this.pdsFhirRestTemplate = pdsFhirRestTemplate;
        this.pdsFhirEndpoint = pdsFhirEndpoint;
    }

    public String requestPdsRecordByNhsNumber(String nhsNumber) {
        String path = "Patient/" + nhsNumber;
        log.info("Sending request to pds for patient");
        pdsFhirRestTemplate.getForEntity(pdsFhirEndpoint + path, String.class);
        log.info("Successful request pds record for patient");
        return "OK";
    }
}
