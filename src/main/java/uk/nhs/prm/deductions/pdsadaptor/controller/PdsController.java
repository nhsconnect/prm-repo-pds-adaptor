package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

@Controller
@RequestMapping("patients")
@RequiredArgsConstructor
@Slf4j
public class PdsController {

    private final PdsService pdsService;

    @GetMapping("/{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PdsResponse> getPatientGpStatus(@PathVariable("nhsNumber") String nhsNumber) {
        log.info("Request for pds record received");
        return new ResponseEntity<>(pdsService.getPatientGpStatus(nhsNumber), HttpStatus.OK);
    }
}
