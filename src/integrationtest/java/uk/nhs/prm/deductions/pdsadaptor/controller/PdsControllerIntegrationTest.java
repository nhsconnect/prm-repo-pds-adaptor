package uk.nhs.prm.deductions.pdsadaptor.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
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
    public void shouldCallGetCurrentTokenAndGetAccessTokenWhenUnAuthorized() throws IOException {
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
            .willReturn(aResponse().withBody(getString())));


        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet("http://localhost:8080/Patient/123");
        httpClient.execute(request);

        ResponseEntity<HttpEntity> exchange = restTemplate.exchange(
            createURLWithPort("/patients/123"), HttpMethod.GET, null, HttpEntity.class);

        assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    private String getString() {
        String json = "{\n" +
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
        return json;
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

}
