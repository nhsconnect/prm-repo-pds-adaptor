package uk.nhs.prm.deductions.pdsadaptor;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;

import static net.logstash.logback.argument.StructuredArguments.kv;

@SpringBootApplication
@Slf4j
public class PdsAdaptorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdsAdaptorApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() {
        log.info("wow, spring is so great");
        log.info("wowsers, spring is so great", kv("what", "spring"), kv("evaluation", "great"));
        var map = new HashMap<String, String>();
        map.put("what", "spring");
        map.put("evaluation", "great");
        log.info("bejeez, spring is so great", map);
        var asJson = new JSONObject(map).toString();
        log.info("bejeez, spring is so great", asJson);
    }

}
