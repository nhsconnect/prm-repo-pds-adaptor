package uk.nhs.prm.deductions.pdsadaptor.client.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashMap;

import static java.lang.String.format;

@Slf4j
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public NotFoundException(String errorMessage, HttpStatusCodeException exception) {
        super(errorMessage, exception);
        logJson(errorMessage, "error_response", exception.getResponseBodyAsString());
    }

    private void logJson(String message, String jsonFieldName, String json) {
        log.info(Markers.appendRaw(jsonFieldName, withoutLinebreaks(json)), message);
    }

    private String withoutLinebreaks(String json) {
        var mapper = new ObjectMapper();
        try {
            var data = mapper.readValue(json, HashMap.class);
            return new JSONObject(data).toJSONString();
        }
        catch (JsonProcessingException e) {
            return json;
        }
    }
}