package uk.nhs.prm.deductions.pdsadaptor.service;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.paginators.GetParametersByPathIterable;

import java.util.HashMap;
import java.util.Map;

public class ReadSSMParameter {

    private final SsmClient ssmClient;

    public ReadSSMParameter(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

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

        GetParametersByPathIterable parametersByPathIterable = ssmClient.getParametersByPathPaginator(getParametersByPathRequest);

        parametersByPathIterable.stream().iterator().forEachRemaining(p -> {
            p.parameters().forEach(s->apiKeyMap.put(s.name(),s.value()));
        });

        return apiKeyMap;
    }

}