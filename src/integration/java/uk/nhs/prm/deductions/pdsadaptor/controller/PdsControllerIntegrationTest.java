package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import uk.nhs.prm.deductions.pdsadaptor.model.SuspendedPatientStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WireMockInitializer.class})
public class PdsControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    public void shouldCallGetCurrentTokenAndGetAccessTokenWhenUnauthorized() {
        stubFor(get(urlMatching("/Patient/9691927179"))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(pdsNotSuspendedPatientResponse())));

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
    public void shouldReturnValidDeceasedPatientResponseWhenPdsFhirResponseHAsDeceasedDateTIme() {
        stubFor(get(urlMatching("/Patient/9691927179"))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));

        stubFor(get(urlMatching("/Patient/9691927179"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(pdsDeceasedPatientResponse())));

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
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));

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
    public void shouldHandle4xxErrorsFromPdsFhirAndReturn400Status() {
        stubFor(post(urlMatching("/access-token"))
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));

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
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));

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
        var requestBody = "{\n" +
                "  \"previousGp\": \"A1234\",\n" +
                "  \"recordETag\": \"W/\\\"5\\\"\"\n" +
                "}";

        var pdsRequstBody = "{\n" +
                "  \"patches\": [\n" +
                "    {\n" +
                "      \"op\": \"add\",\n" +
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
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(pdsRequstBody))
                .willReturn(ResponseDefinitionBuilder.like(ResponseDefinition.notAuthorised())));

        stubFor(post(urlMatching("/access-token"))
                .willReturn(
                        aResponse()
                                .withBody("{\"access_token\": \"accessToken\",\n" +
                                        " \"expires_in\": \"599\",\n" +
                                        " \"token_type\": \"Bearer\"}")));


        stubFor(patch(urlMatching("/Patient/9693797493"))
                .withHeader("Authorization", matching("Bearer accessToken"))
                .withHeader("If-Match", matching("W/\"5\""))
                .withHeader("Content-Type", containing("application/json-patch+json"))
                .withRequestBody(equalToJson(pdsRequstBody))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "W/\"6\"")
                        .withBody(suspendedPatientWithManagingOrganisationResponse())));

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

    private HttpHeaders createHeaders() {
        var headers = new HttpHeaders();
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

    private String pdsDeceasedPatientResponse() {
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
                "              \"valueString\": \"5901640\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-AddressKey\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"HwQJS\",\n" +
                "      \"line\": [\n" +
                "        \"10 CRECY CLOSE\",\n" +
                "        \"DERBY\"\n" +
                "      ],\n" +
                "      \"period\": {\n" +
                "        \"start\": \"2010-06-29\"\n" +
                "      },\n" +
                "      \"postalCode\": \"DE22 3JU\",\n" +
                "      \"use\": \"home\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"birthDate\": \"1977-03-27\",\n" +
                "  \"deceasedDateTime\": \"2010-06-28T00:00:00+00:00\",\n" +
                "  \"extension\": [\n" +
                "    {\n" +
                "      \"extension\": [\n" +
                "        {\n" +
                "          \"url\": \"deathNotificationStatus\",\n" +
                "          \"valueCodeableConcept\": {\n" +
                "            \"coding\": [\n" +
                "              {\n" +
                "                \"code\": \"2\",\n" +
                "                \"display\": \"Formal - death notice received from Registrar of Deaths\",\n" +
                "                \"system\": \"https://fhir.hl7.org.uk/CodeSystem/UKCore-DeathNotificationStatus\",\n" +
                "                \"version\": \"1.0.0\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"url\": \"systemEffectiveDate\",\n" +
                "          \"valueDateTime\": \"2013-05-23T00:00:00+00:00\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-DeathNotificationStatus\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"url\": \"https://fhir.hl7.org.uk/StructureDefinition/Extension-UKCore-NominatedPharmacy\",\n" +
                "      \"valueReference\": {\n" +
                "        \"identifier\": {\n" +
                "          \"system\": \"https://fhir.nhs.uk/Id/ods-organization-code\",\n" +
                "          \"value\": \"FL015\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"gender\": \"unknown\",\n" +
                "  \"generalPractitioner\": [\n" +
                "    {\n" +
                "      \"id\": \"mvtpt\",\n" +
                "      \"identifier\": {\n" +
                "        \"period\": {\n" +
                "          \"start\": \"1983-03-01\"\n" +
                "        },\n" +
                "        \"system\": \"https://fhir.nhs.uk/Id/ods-organization-code\",\n" +
                "        \"value\": \"D81015\"\n" +
                "      },\n" +
                "      \"type\": \"Organization\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"id\": \"9453740586\",\n" +
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
                "      \"value\": \"9453740586\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"security\": [\n" +
                "      {\n" +
                "        \"code\": \"U\",\n" +
                "        \"display\": \"unrestricted\",\n" +
                "        \"system\": \"http://terminology.hl7.org/CodeSystem/v3-Confidentiality\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"versionId\": \"2\"\n" +
                "  },\n" +
                "  \"name\": [\n" +
                "    {\n" +
                "      \"family\": \"PENSON\",\n" +
                "      \"given\": [\n" +
                "        \"HEADLEY\",\n" +
                "        \"TED\"\n" +
                "      ],\n" +
                "      \"id\": \"JtUky\",\n" +
                "      \"period\": {\n" +
                "        \"start\": \"1994-08-23\"\n" +
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