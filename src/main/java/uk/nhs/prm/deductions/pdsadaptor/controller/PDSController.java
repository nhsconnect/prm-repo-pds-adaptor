package uk.nhs.prm.deductions.pdsadaptor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PDSController {
    @GetMapping("/patients/{nhsNumber}")
    @ResponseBody
    public Object retrieveDemographics(@PathVariable("nhsNumber") String nhsNumber){

        return "basic";
    }
}
