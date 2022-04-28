package uk.nhs.prm.deductions.pdsadaptor.client;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.PdsFhirClientExceptionFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class PdsFhirPatchRejectionInterpreter {
    public static boolean isRejectionDueToNotMakingChanges(HttpStatusCodeException exception) {
        if (!exception.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            return false;
        }

        return extractPatchRejectionDiagnostics(exception).contains("Provided patch made no changes to the resource");
    }

    private static String extractPatchRejectionDiagnostics(HttpStatusCodeException exception) {
        try {
            return new JSONObject(exception.getResponseBodyAsString(UTF_8))
                    .getJSONArray("issue")
                    .getJSONObject(0)
                    .getString("diagnostics");

        } catch (JSONException jsonException) {
            log.debug("Patch rejection has no diagnostics");
            return "";
        }
    }
}
