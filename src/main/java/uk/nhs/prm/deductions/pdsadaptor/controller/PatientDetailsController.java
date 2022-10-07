package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.security.Principal;

@RestController
@RequestMapping("/patient-details")
@AllArgsConstructor
@Slf4j
public class PatientDetailsController {

    PdsService pdsService;

    @GetMapping("{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public String getPatientDetails(@PathVariable("nhsNumber") @NotBlank @Size(max = 10, min = 10) String nhsNumber, Principal principal){
        log.info("Request for get patient details received from {}", principal.getName());
        pdsService.getPatientDetails(nhsNumber);
        return null;
    }
}
