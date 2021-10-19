package uk.nhs.prm.deductions.pdsadaptor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterType;

import java.util.Arrays;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadSSMParameterTest {

    @Autowired
    private ReadSSMParameter ssmParamService;

    @Test
    void shouldCallGetParameterByPath(){

        SsmClient ssmClient = Mockito.mock(SsmClient.class);
        ssmParamService = new ReadSSMParameter(ssmClient);

        GetParametersByPathRequest getParametersByPathRequestForService = GetParametersByPathRequest.builder()
                .withDecryption(Boolean.TRUE)
                .path("/repo/" + "local" + "/user-input/api-keys/pds-adaptor/")
                .build();

        GetParametersByPathRequest getParametersByPathRequestForUser = GetParametersByPathRequest.builder()
                .withDecryption(Boolean.TRUE)
                .path("/repo/" + "local" + "/user-input/api-keys/pds-adaptor/api-key-user")
                .build();

        Parameter user1 = Parameter.builder().name("user1").type(ParameterType.SECURE_STRING).value("12345").build();
        Parameter user2 = Parameter.builder().name("user2").type(ParameterType.SECURE_STRING).value("12345").build();

        Parameter service1 = Parameter.builder().name("serevice1").type(ParameterType.SECURE_STRING).value("54321").build();

        GetParametersByPathResponse responseForUser = GetParametersByPathResponse.builder().parameters(Arrays.asList(user1, user2)).build();
        GetParametersByPathResponse responseForService = GetParametersByPathResponse.builder().parameters(Arrays.asList(service1)).build();

        when(ssmClient.getParametersByPath(getParametersByPathRequestForService)).thenReturn(responseForService);
        when(ssmClient.getParametersByPath(getParametersByPathRequestForUser)).thenReturn(responseForUser);


        ssmParamService.getApiKeys("local");
    }

}
