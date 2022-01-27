package uk.nhs.prm.deductions.pdsadaptor.model.Exceptions;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ExceptionFactory {

    public static RuntimeException createException(HttpStatusCodeException exception) {
        if (exception.getStatusCode().equals(HttpStatus.FORBIDDEN) || exception.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            return new AuthException(exception);
        }
        if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new NotFoundException("PDS FHIR Request failed - Patient not found");
        }
        if (exception.getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
            return new TooManyRequestsException();
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Received status code: " + exception.getStatusCode() + " from PDS FHIR");
        }
        if (exception.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            return new ServiceUnavailableException();
        }
        return new PdsFhirRequestException(exception);
    }

    public static RuntimeException createPatchException(HttpStatusCodeException exception) {
        PdsFhirPatchInvalidException pdsFhirPatchInvalidException = handlePatchException(exception);
        return Objects.requireNonNullElseGet(pdsFhirPatchInvalidException, () -> createException(exception));
    }

    public static PdsFhirPatchInvalidException handlePatchException(HttpStatusCodeException exception) {
        if (exception.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            try {
                String diagnosticsValue = new JSONObject(exception.getResponseBodyAsString(UTF_8))
                        .getJSONArray("issue")
                        .getJSONObject(0)
                        .getString("diagnostics");

                if (diagnosticsValue.contains("Provided patch made no changes to the resource")) {
                    return new PdsFhirPatchInvalidException();
                }
            } catch (JSONException jsonException) {
                log.debug("Not invalid patch exception so ignoring");
            }
        }
        return null;
    }
}
