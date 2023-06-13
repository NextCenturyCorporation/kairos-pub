package com.ncc.kairos.moirai.zeus.services;

import com.google.gson.Gson;
import com.ncc.kairos.moirai.zeus.dao.ZeusSettingRepository;
import com.ncc.kairos.moirai.zeus.exceptions.UnaccessibleEndpointException;
import com.ncc.kairos.moirai.zeus.model.ValidationResponse;
import com.ncc.kairos.moirai.zeus.model.ZeusSetting;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

/**
 * AWS Connector class to get newly created instances when provisioning clotho on AWS.
 *
 * @author Lion Tamer
 */
@Service
public class ValidationService {

    @Autowired
    ZeusSettingRepository zeusSettingRepository;

    private final RestTemplate restTemplate;

    public ValidationService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public Boolean shouldValidate() {
        ZeusSetting shouldRun = zeusSettingRepository.findByName(Constants.ZS_VALIDATION_SHOULDRUN);
        if (shouldRun != null && shouldRun.getValue().equalsIgnoreCase("TRUE")) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public ValidationResponse runValidation(String input) throws UnaccessibleEndpointException {
        return runValidation(input.getBytes(StandardCharsets.UTF_8));
    }

    public ValidationResponse runValidation(byte[] input) throws UnaccessibleEndpointException {
        String endpoint = zeusSettingRepository.findByName(Constants.ZS_VALIDATION_ENDPOINT).getValue();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/ld+json");
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(input, headers);

        try {
            return this.restTemplate.postForObject(endpoint, requestEntity, ValidationResponse.class);
        }  catch (HttpClientErrorException.NotFound e) { // Path not found on host
            throw new UnaccessibleEndpointException("Unable to reach " + endpoint);
        } catch (ResourceAccessException e) { // Host not found
            throw new UnaccessibleEndpointException("Unable to reach host " + endpoint);
        } catch (HttpClientErrorException e) {
            ValidationResponse response = null;
            String message = e.getResponseBodyAsString();
            if (message.contains("errorsList") && message.contains("warningsList")) {
                response = new Gson().fromJson(message, ValidationResponse.class);
            }

            if (response == null) {
                response = new ValidationResponse()
                        .warningsList(new ArrayList<>())
                        .errorsList(Collections.singletonList(String.format("Error: %s %s %s", e.getRawStatusCode(), e.getStatusCode().getReasonPhrase(), e.getStatusText())));
            }

            return response;
        } catch (Exception e) { // Such as not proper json
            return new ValidationResponse()
                    .warningsList(new ArrayList<>())
                    .errorsList(Collections.singletonList("Error: " + e.getLocalizedMessage()));
        }
    }


}
