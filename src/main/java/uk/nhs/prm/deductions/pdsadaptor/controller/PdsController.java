package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@RestController
@RequestMapping("suspended-patient-status")
@RequiredArgsConstructor
@Slf4j
public class PdsController {

    private final PdsService pdsService;

    @GetMapping("/{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public SuspendedPatientStatus getPatientGpStatus(@PathVariable("nhsNumber") @NotBlank @Size(max = 10, min = 10) String nhsNumber) {
        log.info("Request for pds record received");
        return pdsService.getPatientGpStatus(nhsNumber);
    }
}
