package uk.nhs.prm.deductions.pdsadaptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrmDeductionsPdsAdaptorApplication {

    private static Logger logger = LogManager.getLogger(PrmDeductionsPdsAdaptorApplication.class);

    public static void main(String[] args) {

        try{
            logger.info("about to start");
            SpringApplication.run(PrmDeductionsPdsAdaptorApplication.class, args);
        }catch (Exception e){
            logger.error("something happened" + e.getMessage());
        }

    }

}
