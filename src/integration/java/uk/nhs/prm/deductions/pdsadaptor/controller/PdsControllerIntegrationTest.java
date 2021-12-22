package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
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
        stubFor(get(urlMatching("/Patient/9691927179"))
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

        stubFor(get(urlMatching("/Patient/9691927179"))
            .inScenario("Get PDS Record")
            .whenScenarioStateIs("Token Generated")
            .withHeader("Authorization", matching("Bearer accessToken"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("ETag", "W/\"6\"")
                .withBody(pdsNotSuspendedPatientResponse())));

        ResponseEntity<SuspendedPatientStatus> response = restTemplate.exchange(
            createURLWithPort("/suspended-patient-status/9691927179"), HttpMethod.GET,
            new HttpEntity<String>(createHeaders()), SuspendedPatientStatus.class);

        SuspendedPatientStatus body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getCurrentOdsCode()).isEqualTo("A20047");
        assertThat(body.getIsSuspended()).isEqualTo(false);
        assertThat(body.getManagingOrganisation()).isNull();
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
    }

    @Test
    public void shouldSendUpdateForManagingOrganisationToPds() {
        String requestBody = "{\n" +
            "  \"previousGp\": \"A1234\",\n" +
            "  \"recordETag\": \"W/\\\"5\\\"\"\n" +
            "}";

        String pdsRequstBody = "{\n" +
            "  \"patches\": [\n" +
            "    {\n" +
            "      \"op\": \"replace\",\n" +
            "      \"path\": \"/managingOrganization\",\n" +
            "      \"value\": {\n" +
            "        \"type\": \"Organization\",\n" +
            "        \"identifier\": {\n" +
            "          \"system\": \"https://fhir.nhs.uk/Id/ods-organization-code\",\n" +
            "          \"value\": \"A1234\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        stubFor(patch(urlMatching("/Patient/9693797493"))
            .inScenario("Update PDS Record")
            .whenScenarioStateIs(STARTED)
            .withHeader("If-Match", matching("W/\"5\""))
            .withHeader("Content-Type", containing("application/json-patch+json"))
            .withRequestBody(equalToJson(pdsRequstBody))
            .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
            .inScenario("Update PDS Record")
            .whenScenarioStateIs(STARTED)
            .willReturn(
                aResponse()
                    .withBody("{\"access_token\": \"accessToken\",\n" +
                        " \"expires_in\": \"599\",\n" +
                        " \"token_type\": \"Bearer\"}"))
            .willSetStateTo("Token Generated"));


        stubFor(patch(urlMatching("/Patient/9693797493"))
            .inScenario("Update PDS Record")
            .whenScenarioStateIs("Token Generated")
            .withHeader("Authorization", matching("Bearer accessToken"))
            .withHeader("If-Match", matching("W/\"5\""))
            .withHeader("Content-Type", containing("application/json-patch+json"))
            .withRequestBody(equalToJson(pdsRequstBody))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("ETag", "W/\"6\"")
                .withBody(suspendedPatientWithManagingOrganisationResponse())));

        ResponseEntity<SuspendedPatientStatus> response = restTemplate.exchange(
            createURLWithPort("/suspended-patient-status/9693797493"),
            HttpMethod.PUT, new HttpEntity<>(requestBody, createHeaders()), SuspendedPatientStatus.class);

        SuspendedPatientStatus body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getCurrentOdsCode()).isNull();
        assertThat(body.getIsSuspended()).isEqualTo(true);
        assertThat(body.getManagingOrganisation()).isEqualTo("A1234");
        assertThat(body.getRecordETag()).isEqualTo("W/\"6\"");
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("admin", "admin");
        headers.add("traceId", "test-trace-id");
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String pdsNotSuspendedPatientResponse() {
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

    private String suspendedPatientWithManagingOrganisationResponse() {
        return "{\n" +
            "    \"address\": [\n" +
            "        {\n" +
            "            \"extension\": [\n" +
            "                {\n" +
            "                    \"extension\": [\n" +
            "                        {\n" +
            "                            \"url\": \"type\",\n" +
            "                            \"valueCoding\": {\n" +
            "                                \"code\": \"PAF\",\n" +
            "                                \"system\": \"https://fhir.hl7.org.uk/CodeSystem/UKCore-AddressKeyType\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        {\n" +
            "                            \"url\": \"value\",\n" +
            "                            \"valueString\": \"19974416\"\n" +
            "                        }\n" +
            "                    ],\n" +
            "                    \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-AddressKey\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"id\": \"dBxTY\",\n" +
            "            \"line\": [\n" +
            "                \"12 AVENUE VIVIAN\",\n" +
            "                \"SCUNTHORPE\"\n" +
            "            ],\n" +
            "            \"period\": {\n" +
            "                \"start\": \"2003-11-24\"\n" +
            "            },\n" +
            "            \"postalCode\": \"DN15 8JW\",\n" +
            "            \"use\": \"home\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"birthDate\": \"1992-01-31\",\n" +
            "    \"gender\": \"male\",\n" +
            "    \"id\": \"9693797493\",\n" +
            "    \"identifier\": [\n" +
            "        {\n" +
            "            \"extension\": [\n" +
            "                {\n" +
            "                    \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NHSNumberVerificationStatus\",\n" +
            "                    \"valueCodeableConcept\": {\n" +
            "                        \"coding\": [\n" +
            "                            {\n" +
            "                                \"code\": \"01\",\n" +
            "                                \"display\": \"Number present and verified\",\n" +
            "                                \"system\": \"https://fhir.hl7.org.uk/CodeSystem/UKCore-NHSNumberVerificationStatus\",\n" +
            "                                \"version\": \"1.0.0\"\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                }\n" +
            "            ],\n" +
            "            \"system\": \"https://fhir.nhs.uk/Id/nhs-number\",\n" +
            "            \"value\": \"9693797493\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"managingOrganization\": {\n" +
            "        \"identifier\": {\n" +
            "            \"period\": {\n" +
            "                \"start\": \"2021-12-03\"\n" +
            "            },\n" +
            "            \"system\": \"https://fhir.nhs.uk/Id/ods-organization-code\",\n" +
            "            \"value\": \"A1234\"\n" +
            "        },\n" +
            "        \"type\": \"Organization\"\n" +
            "    },\n" +
            "    \"meta\": {\n" +
            "        \"security\": [\n" +
            "            {\n" +
            "                \"code\": \"U\",\n" +
            "                \"display\": \"unrestricted\",\n" +
            "                \"system\": \"http://terminology.hl7.org/CodeSystem/v3-Confidentiality\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"versionId\": \"6\"\n" +
            "    },\n" +
            "    \"name\": [\n" +
            "        {\n" +
            "            \"family\": \"SPARKS\",\n" +
            "            \"given\": [\n" +
            "                \"John\"\n" +
            "            ],\n" +
            "            \"id\": \"pYssk\",\n" +
            "            \"period\": {\n" +
            "                \"start\": \"2010-01-10\"\n" +
            "            },\n" +
            "            \"prefix\": [\n" +
            "                \"MR\"\n" +
            "            ],\n" +
            "            \"use\": \"usual\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"resourceType\": \"Patient\"\n" +
            "}";
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

}
