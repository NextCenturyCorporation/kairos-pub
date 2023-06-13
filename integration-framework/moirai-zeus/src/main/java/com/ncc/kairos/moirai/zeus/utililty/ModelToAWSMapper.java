package com.ncc.kairos.moirai.zeus.utililty;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.ncc.kairos.moirai.zeus.model.ServiceAwsInstance;
import com.ncc.kairos.moirai.zeus.model.ServiceEndpoint;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS Connector class to get newly created instances when provisioning clotho on AWS.
 * @author Lion Tamer
 */
public class ModelToAWSMapper {

    public static ServiceAwsInstance getServiceAwsInstance(Instance instance) {
        ServiceAwsInstance serviceAwsInstance = new ServiceAwsInstance();
        serviceAwsInstance.setInstanceId(instance.getInstanceId());
        return serviceAwsInstance;
    }

    public static List<ServiceEndpoint> getServiceEndpointsInTags(List<Tag> tags) {
        List<ServiceEndpoint> endpoints = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag.getKey().toLowerCase().contains("endpoint")) {
                ServiceEndpoint endpoint = new ServiceEndpoint();
                endpoint.setName(tag.getKey());
                endpoint.setUri(tag.getValue());
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }
}
