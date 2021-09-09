package uk.nhs.prm.deductions.pdsadaptor.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.nhs.prm.deductions.pdsadaptor.service.PdsService;

@Controller
@RequestMapping("patients")
@AllArgsConstructor
public class PdsController {

    private PdsService pdsService;

    @GetMapping("/{nhsNumber}")
    @ResponseStatus(HttpStatus.OK)
    public String getPatientGpStatus(@PathVariable("nhsNumber") String nhsNumber)  {
        return pdsService.getPatientGpStatus(nhsNumber);
    }
}
