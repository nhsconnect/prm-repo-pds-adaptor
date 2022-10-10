package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.nhs.prm.deductions.pdsadaptor.model.PatientTraceInformation;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.security.Principal;

@RestController
@RequestMapping("/patient-trace-information")
@AllArgsConstructor
@Slf4j
public class PatientTraceInformationController {

    PdsService pdsService;

    @GetMapping("{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public PatientTraceInformation getPatientTraceInformation(@PathVariable("nhsNumber") @NotBlank @Size(max = 10, min = 10) String nhsNumber, Principal principal){
        log.info("Request for patient trace information received from {}", principal.getName());
        return pdsService.getPatientTraceInformation(nhsNumber);
    }
}
