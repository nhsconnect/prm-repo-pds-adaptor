package uk.nhs.prm.deductions.pdsadaptor;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
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
        var map = new HashMap<String, String>();
        var superMap = new HashMap<String, Object>();
        map.put("what", "spring");
        map.put("evaluation", "great");
        superMap.put("nested", map);
        log.info("bejeez, spring is so great", map);
        log.info("amaze, spring is so great {}", map);
        log.info("coool, spring is so great {}", superMap);
        var asJson = new JSONObject(map).toString();
        log.info("wowzie, spring is so great", asJson);
        var superJson = new JSONObject(superMap).toString();
        log.info("beleev, spring is so great", superJson);

        log.info(Markers.appendRaw("details", superJson), "mark my words");
        log.info(Markers.appendEntries(superMap), "mark my wurdz");
    }

}
