package uk.nhs.prm.deductions.pdsadaptor.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

    @GetMapping("/{name}")
    public String healthCheck(@PathVariable String name) {
        return "Hello " + name;
    }
}
