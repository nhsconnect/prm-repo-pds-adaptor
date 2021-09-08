package uk.nhs.prm.deductions.pdsadaptor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.prm.deductions.pdsadaptor.service.PDSFHIRClient;

@RestController
public class PDSController {
    PDSFHIRClient pdsFHIRClient;
    public PDSController (PDSFHIRClient pdsFHIRClient){
        this.pdsFHIRClient = pdsFHIRClient;
    }
    @Value("$jwtPrivateKey")
    String jwtPrivateKey;

    @GetMapping("/patients/{nhsNumber}")
    @ResponseBody
    public String retrieveDemographics(@PathVariable("nhsNumber") String nhsNumber) throws Exception {

        return jwtPrivateKey;
    }
}
