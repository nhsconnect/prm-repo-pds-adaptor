package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import java.io.IOException;

@Controller
@RequestMapping("patients")
@AllArgsConstructor
@Slf4j
public class PdsController {

    private PdsService pdsService;

    @GetMapping("/{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getPatientGpStatus(@PathVariable("nhsNumber") String nhsNumber) throws IOException {
        log.info("Request for pds record received");
        return pdsService.getPatientGpStatus(nhsNumber);
    }
}
