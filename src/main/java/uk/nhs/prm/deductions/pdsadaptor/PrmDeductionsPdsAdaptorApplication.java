package uk.nhs.prm.deductions.pdsadaptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrmDeductionsPdsAdaptorApplication {

    public static void main(String[] args) {

        try{
            System.out.println("---------About to run");
            SpringApplication.run(PrmDeductionsPdsAdaptorApplication.class, args);
        }catch (Exception e){

            System.out.println(e.getMessage());
        }

    }

}
