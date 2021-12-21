package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WireMockInitializer.class})
public class PdsControllerIntegrationTest {

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @AfterEach
    public void afterEach() {
        this.wireMockServer.resetAll();
    }

    @Test
    public void shouldCallGetCurrentTokenAndGetAccessTokenWhenUnAuthorized() {
        stubFor(get(urlMatching("/Patient/123"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs(STARTED)
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}"))
                .willSetStateTo("Token Generated"));

        stubFor(get(urlMatching("/Patient/123"))
                .inScenario("Get PDS Record")
                .whenScenarioStateIs("Token Generated")
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(getString())));

        ResponseEntity<SuspendedPatientStatus> response = restTemplate.exchange(
                createURLWithPort("/suspended-patient-status/123"), HttpMethod.GET, new HttpEntity<String>("parameters", createHeaders("admin", "admin")), SuspendedPatientStatus.class);

        SuspendedPatientStatus body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getCurrentOdsCode()).isEqualTo("A20047");
        assertThat(body.getIsSuspended()).isEqualTo(false);
        assertThat(body.getManagingOrganisation()).isNull();
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
    }

    private HttpHeaders createHeaders(String username, String password) {
        String plainCreds = username.concat(":").concat(password);
        byte[] plainCredsBytes = plainCreds.getBytes(StandardCharsets.UTF_8);
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + new String(base64CredsBytes,
                StandardCharsets.UTF_8));
        headers.add("traceId", "test-trace-id");

        return headers;
    }

    private String getString() {
        return "{\n" +
                "  \"address\": [\n" +
                "    {\n" +
                "      \"extension\": [\n" +
                "        {\n" +
                "          \"extension\": [\n" +
                "            {\n" +
                "              \"url\": \"type\",\n" +
                "              \"valueCoding\": {\n" +
                "                \"code\": \"PAF\",\n" +
                "                \"system\": \"https://fhir.hl7.org.uk/CodeSystem/UKCore-AddressKeyType\"\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"url\": \"value\",\n" +
                "              \"valueString\": \"6292549\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-AddressKey\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"RfSFs\",\n" +
                "      \"line\": [\n" +
                "        \"1 Whitehall \",\n" +
                "        \"Leeds \",\n" +
                "        \"West Yorkshire \"\n" +
                "      ],\n" +
                "      \"period\": {\n" +
                "        \"start\": \"1993-10-12\"\n" +
                "      },\n" +
                "      \"postalCode\": \"LS1 4HR\",\n" +
                "      \"use\": \"home\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"birthDate\": \"1982-06-08\",\n" +
                "  \"gender\": \"male\",\n" +
                "  \"generalPractitioner\": [\n" +
                "    {\n" +
                "      \"id\": \"OwJvS\",\n" +
                "      \"identifier\": {\n" +
                "        \"period\": {\n" +
                "          \"start\": \"1998-01-22\"\n" +
                "        },\n" +
                "        \"system\": \"https://fhir.nhs.uk/Id/ods-organization-code\",\n" +
                "        \"value\": \"A20047\"\n" +
                "      },\n" +
                "      \"type\": \"Organization\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"id\": \"9691927179\",\n" +
                "  \"identifier\": [\n" +
                "    {\n" +
                "      \"extension\": [\n" +
                "        {\n" +
                "          \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus\",\n" +
                "          \"valueCodeableConcept\": {\n" +
                "            \"coding\": [\n" +
                "              {\n" +
                "                \"code\": \"01\",\n" +
                "                \"display\": \"Number present and verified\",\n" +
                "                \"system\": \"https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus\",\n" +
                "                \"version\": \"1.0.0\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"system\": \"https://fhir.nhs.uk/Id/nhs-number\",\n" +
                "      \"value\": \"9691927179\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"security\": [\n" +
                "      {\n" +
                "        \"code\": \"U\",\n" +
                "        \"display\": \"unrestricted\",\n" +
                "        \"system\": \"https://www.hl7.org/fhir/valueset-security-labels.html\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"versionId\": \"1\"\n" +
                "  },\n" +
                "  \"name\": [\n" +
                "    {\n" +
                "      \"family\": \"XDJ-JUN-ALLOC-ALGO-MULTI\",\n" +
                "      \"given\": [\n" +
                "        \"ALEKSANDER\",\n" +
                "        \"Wilbur\"\n" +
                "      ],\n" +
                "      \"id\": \"SYEPl\",\n" +
                "      \"period\": {\n" +
                "        \"start\": \"2015-08-25\"\n" +
                "      },\n" +
                "      \"prefix\": [\n" +
                "        \"MR\"\n" +
                "      ],\n" +
                "      \"use\": \"usual\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"resourceType\": \"Patient\"\n" +
                "}";
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

}
