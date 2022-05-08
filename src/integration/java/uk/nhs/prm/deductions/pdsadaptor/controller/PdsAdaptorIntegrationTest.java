package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WireMockInitializer.class})
public class PdsAdaptorIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    public void shouldCallGetCurrentTokenAndGetAccessTokenWhenUnauthorized() {
        stubFor(get(urlMatching("/Patient/9691927179"))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(loadResource("fhir-responses/not-suspended-patient.json"))));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        var body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getCurrentOdsCode()).isEqualTo("A20047");
        assertThat(body.getIsSuspended()).isEqualTo(false);
        assertThat(body.getManagingOrganisation()).isNull();
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
    }

    @Test
    public void shouldReturnValidDeceasedPatientResponseWhenPdsFhirResponseHasDeceasedDateTIme() {
        stubFor(get(urlMatching("/Patient/9691927179"))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(loadResource("fhir-responses/deceased-patient.json"))));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        var body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getIsSuspended()).isEqualTo(null);
        assertThat(body.getManagingOrganisation()).isNull();
        assertThat(body.getCurrentOdsCode()).isNull();
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
        assertThat(body.getIsDeceased()).isEqualTo(true);
    }

    @Test
    public void shouldHandle5xxErrorsFromPdsFhirAndReturn503Status() {
        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse().withStatus(503)));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldHandleAuthErrorsFromPdsFhirAndReturn503Status() {
        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withStatus(403)));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldHandleOther4xxErrorsFromPdsFhirAndReturn400Status() {
        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse().withStatus(400)));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void shouldHandle429TooManyRequestErrorsFromPdsFhirAndReturn503Status() {
        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse().withStatus(429)));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
                new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldSendUpdateForManagingOrganisationToPds() {
        var requestBody = new JSONObject()
                .put("previousGp", "A1234")
                .put("recordETag", "W/\"5\"").toString();

        stubFor(patch(urlMatching("/Patient/9693797493"))
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(fhirPatchJsonToUpdateMofTo("A1234")))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(patch(urlMatching("/Patient/9693797493"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(fhirPatchJsonToUpdateMofTo("A1234")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(loadResource("fhir-responses/suspended-patient-with-managing-organisation.json"))));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9693797493"),
                HttpMethod.PUT, new HttpEntity<>(requestBody, createHeaders()), SuspendedPatientStatus.class);

        var body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getNhsNumber()).isEqualTo("9693797493");
        assertThat(body.getCurrentOdsCode()).isNull();
        assertThat(body.getIsSuspended()).isEqualTo(true);
        assertThat(body.getManagingOrganisation()).isEqualTo("A1234");
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
    }

    @Test
    public void shouldRetryWhen503ErrorsFromPdsFhir() {
        var requestBody = new JSONObject()
                .put("previousGp", "A1235")
                .put("recordETag", "W/\"5\"").toString();

        stubFor(post(urlMatching("/access-token"))
                .willReturn(aResponse().withBody(freshAccessToken())));

        stubFor(patch(urlMatching("/Patient/9691927179"))
                .inScenario("Retry Scenario")
                .withHeader("Authorization", matching("Bearer accessToken"))
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(fhirPatchJsonToUpdateMofTo("A1235")))
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(503) // request unsuccessful with status code 500
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("TRIED_ONCE"));

        stubFor(patch(urlMatching("/Patient/9691927179"))
                .inScenario("Retry Scenario")
                .withHeader("Authorization", matching("Bearer accessToken"))
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(fhirPatchJsonToUpdateMofTo("A1235")))
                .whenScenarioStateIs("TRIED_ONCE")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(loadResource("fhir-responses/suspended-patient-with-managing-organisation.json"))));

        var response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/9691927179"),
                HttpMethod.PUT, new HttpEntity<>(requestBody, createHeaders()), SuspendedPatientStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders createHeaders() {
        var headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin");
        headers.add("traceId", "test-trace-id");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String fhirPatchJsonToUpdateMofTo(String newManagingOrganisationOdsCode) {
        var expectedMofUpdatePatchJson = new JSONObject().put("patches", List.of(new JSONObject()
                .put("op", "add")
                .put("path", "/managingOrganization")
                .put("value", new JSONObject()
                        .put("type", "Organization")
                        .put("identifier", new JSONObject()
                                .put("system", "https://fhir.nhs.uk/Id/ods-organization-code")
                                .put("value", newManagingOrganisationOdsCode))))).toString();
        return expectedMofUpdatePatchJson;
    }

    private String loadResource(String filename) {
        try {
            return new String(getClass().getClassLoader().getResourceAsStream(filename).readAllBytes());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String freshAccessToken() {
        return "{\"access_token\": \"accessToken\",\n" +
                " \"expires_in\": \"599\",\n" +
                " \"token_type\": \"Bearer\"}";
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }
}