package uk.nhs.prm.deductions.pdsadaptor.client;

import net.logstash.logback.marker.RawJsonAppendingMarker;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static uk.nhs.prm.deductions.pdsadaptor.client.PdsFhirClientExceptionHandler.handleCommonExceptions;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestLogAppender.addTestLogAppender;

class PdsFhirClientExceptionHandlerTest {

    @Test
    public void shouldStructurallyLogTheResponseBodyForAHttp404() {
        var testLogAppender = addTestLogAppender();

        var responseBody = new HashMap<>() {
            {
                put("some_code", "yo code");
                put("some_detail", "hey detail");
            }
        };
        assertThrows(RuntimeException.class, () ->
                handleCommonExceptions("some description", createErrorResponse(404, asJson(responseBody))));

        var logged = testLogAppender.getLastLoggedEvent();
        assertThat(logged.getMarker()).isInstanceOf(RawJsonAppendingMarker.class);

        var jsonMarker = (RawJsonAppendingMarker) logged.getMarker();
        assertThat(jsonMarker.getFieldName()).isEqualTo("error_response");

        var loggedJson = (String) jsonMarker.getFieldValue();
        assertThat(loggedJson.contains("some_code")).isTrue();
        assertThat(loggedJson.contains("some_detail")).isTrue();
    }

    @NotNull
    private HttpClientErrorException createErrorResponse(int statusCode, String responseBodyJson) {
        return new HttpClientErrorException(HttpStatus.resolve(statusCode), "error", responseBodyJson.getBytes(UTF_8), UTF_8);
    }

    private String asJson(HashMap<Object, Object> errorResponseBodyContent) {
        return new JSONObject(errorResponseBodyContent).toString();
    }

}