package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClient;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PdsService {

    private final PdsFhirClient pdsFhirClient;

    public ResponseEntity getPatientGpStatus(String nhsNumber) throws IOException {
        return pdsFhirClient.requestPdsRecordByNhsNumber(nhsNumber);
    }
}
