package uk.nhs.prm.deductions.pdsadaptor.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class ReadSSMParameter {

//    Region region = Region.EU_WEST_2;
    private final SsmClient ssmClient;



    public Map<String,String> getApiKeys(String environment) {
        String serviceParamName = "/repo/" + environment + "/user-input/api-keys/pds-adaptor/";
        String userParamName = "/repo/" + environment + "/user-input/api-keys/pds-adaptor/api-key-user";

       Map<String, String> userAndServiceApiKeyMap= new HashMap<>();

        userAndServiceApiKeyMap.putAll(readParamsByPath(serviceParamName));
        userAndServiceApiKeyMap.putAll(readParamsByPath(userParamName));

        ssmClient.close();

        return userAndServiceApiKeyMap;
    }


    private Map<String, String> readParamsByPath(String paramName) {
        Map<String, String> apiKeyMap = new HashMap<>();

        GetParametersByPathRequest getParametersByPathRequest = GetParametersByPathRequest.builder()
                .withDecryption(Boolean.TRUE)
                .path(paramName)
                .build();

        GetParametersByPathResponse parametersByPathResponse = ssmClient.getParametersByPath(getParametersByPathRequest);

        for(Parameter parameter : parametersByPathResponse.parameters()){
            apiKeyMap.put(parameter.name(), parameter.value());
        }
        return apiKeyMap;
    }

}