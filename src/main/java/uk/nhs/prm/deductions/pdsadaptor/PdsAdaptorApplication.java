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

        var superJson = new JSONObject(superMap).toString();

        log.info(Markers.appendRaw("detail", superJson), "log spike: detail from json string");

        var superJsonWithBreaks = "{\"a\":\"aa\"," +
                "\n\"b\":{" +
                    "\n\"c\":\"cc\"}" +
                "\n}";

        log.info(Markers.appendRaw("detail", superJsonWithBreaks), "log spike: detail from json with line breaks");
    }

}
