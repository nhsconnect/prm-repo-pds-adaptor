package uk.nhs.prm.deductions.pdsadaptor.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.UnknownContentTypeException;
import uk.nhs.prm.deductions.pdsadaptor.client.exceptions.*;
import uk.nhs.prm.deductions.pdsadaptor.model.UpdateManagingOrganisationRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatch;
import uk.nhs.prm.deductions.pdsadaptor.model.pdspatchrequest.PdsPatchRequest;
import uk.nhs.prm.deductions.pdsadaptor.model.pdsresponse.PdsResponse;

import java.time.LocalDate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsResponse;
import static uk.nhs.prm.deductions.pdsadaptor.testhelpers.TestData.buildPdsSuspendedResponse;

@ExtendWith(MockitoExtension.class)
class PdsFhirPatchRejectionInterpreterTest {

    private PdsFhirPatchRejectionInterpreter interpreter;

    @BeforeEach
    void setUp() {
        interpreter = new PdsFhirPatchRejectionInterpreter();
    }

    @Test
    void shouldRecogniseRejectionDueToNotSpecifyingChangesInPathWhen400BadRequestIsReturnedWithRelevantDiagnostics() {
        String errorResponse = "{\n" +
                "    \"issue\": [\n" +
                "        {\n" +
                "            \"code\": \"whatever structure\",\n" +
                "            \"details\": \"whatever details\",\n" +
                "            \"diagnostics\": \"Invalid update with error - Invalid patch - Provided patch made no changes to the resource\",\n" +
                "            \"severity\": \"whatever severity\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"resourceType\": \"whatever\"\n" +
                "}";

        var httpException =  new HttpClientErrorException(HttpStatus.BAD_REQUEST, "400 Bad Request", errorResponse.getBytes(UTF_8), UTF_8);

        assertThat(interpreter.isRejectionDueToNotMakingChanges(httpException)).isTrue();
    }

    @Test
    void shouldNotRecogniseAGeneral400BadRequestAsNoChangesRejectionWhenDiagnosticsAreMissing() {
        String errorResponse = "{\n" +
                "    \"issue\": [\n" +
                "        {\n" +
                "            \"code\": \"structure\",\n" +
                "            \"details\": {\n" +
                "                \"coding\": [\n" +
                "                    {\n" +
                "                        \"code\": \"INVALID_UPDATE\",\n" +
                "                        \"display\": \"Update is invalid\",\n" +
                "                        \"system\": \"https://fhir.nhs.uk/R4/CodeSystem/Spine-ErrorOrWarningCode\",\n" +
                "                        \"version\": \"1\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            },\n" +
                "            \"severity\": \"error\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"resourceType\": \"OperationOutcome\"\n" +
                "}";

        var httpException =  new HttpClientErrorException(HttpStatus.BAD_REQUEST, "400 Bad Request", errorResponse.getBytes(UTF_8), UTF_8);

        assertThat(interpreter.isRejectionDueToNotMakingChanges(httpException)).isFalse();
    }

    @Test
    void shouldNotRecogniseAGeneral400BadRequestAsNoChangesRejectionWhenDiagnosticsAreNotSpecificallySayingThat() {
        String errorResponse = "{\n" +
                "    \"issue\": [\n" +
                "        {\n" +
                "            \"code\": \"whatever structure\",\n" +
                "            \"details\": \"whatever details\",\n" +
                "            \"diagnostics\": \"Invalid update with error - Invalid patch - For some other unrecognised reason\",\n" +
                "            \"severity\": \"whatever severity\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"resourceType\": \"whatever\"\n" +
                "}";

        var httpException =  new HttpClientErrorException(HttpStatus.BAD_REQUEST, "400 Bad Request", errorResponse.getBytes(UTF_8), UTF_8);

        assertThat(interpreter.isRejectionDueToNotMakingChanges(httpException)).isFalse();
    }
}
