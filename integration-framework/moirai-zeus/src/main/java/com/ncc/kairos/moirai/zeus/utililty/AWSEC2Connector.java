package com.ncc.kairos.moirai.zeus.utililty;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import com.ncc.kairos.moirai.zeus.services.PropertiesService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class AWSEC2Connector {
    private static final Logger LOGGER = Logger.getLogger(AWSEC2Connector.class.getName());

    @Autowired
    PropertiesService propertiesService;

    public String getVpcId() {
        String vpcId = null;

        Region region = getRegion();

        final AmazonEC2 ec2 = AWSEC2Connector.getEC2SynchronousClient(region.getName());
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        List<String> valuesT1 = new ArrayList<>();
        valuesT1.add("BeachHead");
        Filter filter1 = new Filter("tag:Name", valuesT1);

        DescribeInstancesResult result = ec2.describeInstances(request.withFilters(filter1));
        List<Reservation> reservations = result.getReservations();
        if (reservations != null && !reservations.isEmpty()) {
            vpcId = reservations.get(0).getInstances().get(0).getVpcId();
        }
        return vpcId;
    }

    public Instance getCreatedInstanceByTag(String resourceName, JwtUser user) {
        //If we are in development mode we don't actually create the new instance
        //Instead we will return dummy data.
        if (propertiesService.whichEnvironment() == EnvironmentTier.DEVELOPMENT) {
            Instance dummyInstance = new Instance();
            dummyInstance.setInstanceId(String.format("Local_%s_%s", user.getUsername().toLowerCase(), resourceName.toLowerCase()));

            Tag fakeTag = new Tag("LocalEndpoint", "http://linkToNoWhere.com/");
            dummyInstance.setTags(Arrays.asList(fakeTag));

            return dummyInstance;
        }

        Instance instanceToReturn = null;

        Region region = getRegion();

        final AmazonEC2 ec2 = AWSEC2Connector.getEC2SynchronousClient(region.getName());
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        List<String> valuesT1 = new ArrayList<>();
        valuesT1.add(Constants.AWS_TAG_PREFIX + user.getUsername().toLowerCase() + "_" + resourceName.toLowerCase());
        Filter filter1 = new Filter("tag:Name", valuesT1);

        DescribeInstancesResult result = ec2.describeInstances(request.withFilters(filter1));
        List<Reservation> reservations = result.getReservations();
        if (reservations != null && !reservations.isEmpty()) {
            instanceToReturn = reservations.get(0).getInstances().get(0);
        }
        return instanceToReturn;
    }

    public List<String> getInstanceTypes(List<String> filters) {
       Region region = getRegion();
        
        Filter filter = new Filter("instance-type", filters);
        DescribeInstanceTypeOfferingsRequest  request = new DescribeInstanceTypeOfferingsRequest()
                                                            .withFilters(filter)
                                                            .withLocationType(LocationType.Region);

        final AmazonEC2 ec2 = AWSEC2Connector.getEC2SynchronousClient(region.getName());
        DescribeInstanceTypeOfferingsResult  result = ec2.describeInstanceTypeOfferings(request);
        return result.getInstanceTypeOfferings().stream().map(offering -> offering.getInstanceType())
        .collect(Collectors.toList());
    }

    private static AmazonEC2 getEC2SynchronousClient(String region) {

        ClientConfiguration configuration = new ClientConfiguration();
        AWSCredentialsProvider awsStaticCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        AmazonEC2ClientBuilder ec2ClientBuilder = AmazonEC2ClientBuilder.standard()
                .withCredentials(awsStaticCredentialsProvider).withRegion(region)
                .withClientConfiguration(configuration);

        return ec2ClientBuilder.build();
    }

    private Region getRegion() {

        Region region = null;
        try {
            region = StringUtils.isEmpty(propertiesService.getAwsRegion()) ? Regions.getCurrentRegion() : Region.getRegion(Regions.fromName(propertiesService.getAwsRegion()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Issue connecting to AWS for region, defaulting to us_east_1");
        } finally {
            if (region == null) {
                region = Region.getRegion(Regions.US_EAST_1);
            }
        }
        return region;
    }
    
}
