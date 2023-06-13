package com.ncc.kairos.moirai.zeus.services;

import com.amazonaws.services.ec2.model.Instance;
import com.ncc.kairos.moirai.zeus.dao.ExperimentRepository;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import com.ncc.kairos.moirai.zeus.runner.AnsibleRunner;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.runner.TerraformRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSEC2Connector;
import com.ncc.kairos.moirai.zeus.utililty.ModelToAWSMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User Service layer to handle business logic for Service endpoints.
 *
 * @author Lion Tamer
 */
@org.springframework.stereotype.Service
public class TAServices {

    private static final Logger LOGGER = Logger.getLogger(TAServices.class.getName());

    @Value("${java.cli.wait.timeout}")
    private Integer waitTimeout;

    @Autowired
    private UserServicesService userServicesService;

    @Autowired
    private AWSEC2Connector awsEc2Connector;

    @Autowired
    AnsibleRunner ansibleRunner;

    @Autowired
    ProcessRunner processRunner;

    @Autowired
    TerraformRunner terraformRunner;

    @Autowired
    PropertiesService propertiesService;

    @Autowired
    KairosUserService kairosUserService;

    @Autowired
    ExperimentRepository experimentRepository;

    @Value("${cli.runner.verbosity}")
    private int runnerVerbosity;


    public TAServices() {

    }

    /**
     * Handles taking a cypher query from the request body and passing to a Neo4J
     * repository.
     *
     * @param body    The request body as json with a 'queryString' containing the
     *                cypher query.
     * @param jwtUser that should own this clotho service
     * @return String The response of the query.
     */
    public void provisionClotho(Service body, JwtUser jwtUser) {
        Service returnService = null;

        serviceVerificiation(body.getName());
        try {
            body.status(Constants.SERVICE_STATUS_PENDING);
            body.teamName(jwtUser.getTeamName());
            returnService = this.userServicesService.saveNewService(body, jwtUser);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Constants.RDS_PERSISTANCE_ERROR_MSG + body.toString(), e);

        }

        String response = String.format("Account Name: %1$s  Database Type: %2$s   Database Name: %3$s ",
                jwtUser.getUsername(), body.getSubtype(), body.getName());

        if (StringUtils.isEmpty(body.getName()) || StringUtils.isEmpty(body.getSubtype())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Request:  " + response);
        }
        Map<String, String> extraVars = new HashMap<>();
        extraVars.put("performer_name", jwtUser.getUsername());
        extraVars.put("resource_name", body.getName());
        extraVars.put("database", body.getSubtype());
        LOGGER.log(Level.INFO, "Current Tier: " + propertiesService.whichEnvironment());
        if (propertiesService.whichEnvironment().equals(EnvironmentTier.TESTING)) {
            LOGGER.log(Level.INFO, "Creating it as t3a.micro");
            extraVars.put("aws_instance_type", "t3a.micro");
        }
        extraVars.put("aws_key_pair", "ZeusControlBox");
        extraVars.put("ansible_ssh_private_key_file", "../key-pairs/ZeusControlBox.pem");
        ansibleRunner.runAnsible("provision_clotho_ec2.yml", extraVars, this.runnerVerbosity);

        // Get created Instance
        Instance createdInstance = this.awsEc2Connector.getCreatedInstanceByTag(body.getName(), jwtUser);

        LOGGER.log(Level.INFO, String.format("Created Instance: \n%s", createdInstance.toString()));
        if (createdInstance != null && returnService != null) {
            returnService.addAwsInstancesItem(ModelToAWSMapper.getServiceAwsInstance(createdInstance));
            returnService.setEndpoints(ModelToAWSMapper.getServiceEndpointsInTags(createdInstance.getTags()));

            // TODO this is dummy data and needs to be replaced with the actual generation
            // of a connector.
            List<ServiceDownload> downloads = new ArrayList<>();
            downloads.add(new ServiceDownload().name("Python Connect")
                    .uri("s3://kairos-service-artifacts/connectors/next_century/service_a/connector.py"));
            returnService.setDownloads(downloads);
            returnService.status(Constants.SERVICE_STATUS_ACTIVE);

            this.userServicesService.updateServiceStatus(returnService);

            LOGGER.log(Level.INFO, String.format("Done! Created instance: %1$s", returnService.getAwsInstances()));
        }
    }

    /**
     * Terminates Active services hosted on AWS and updates their internal status to
     * 'Terminated'.
     *
     * @param service service to be turned off and updated
     * @param jwtUser User associated with service
     * @return String The response of the delete.
     */
    public void terminateClothoInstance(@Valid Service service, JwtUser jwtUser) {
        int exitCodeAggregate = 0;
        List<Integer> exitCodes = new ArrayList<>();
        for (ServiceAwsInstance instance : service.getAwsInstances()) {
            if (StringUtils.isEmpty(instance.getInstanceId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Request:  " + service.toString());
            }

            Map<String, String> extraVars = new HashMap<>();
            extraVars.put("id_to_term", instance.getInstanceId());
            Process process = ansibleRunner.runAnsible("terminate_clotho_ec2.yml", extraVars, this.runnerVerbosity);

            exitCodeAggregate += process.exitValue();
            exitCodes.add(process.exitValue());
        }

        // update DB
        if (exitCodeAggregate == 0) {
            service.status(Constants.SERVICE_STATUS_TERMINATED);
            this.userServicesService.updateServiceStatus(service);
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Terminate: exit status code: " + exitCodes);
        }
    }
    
    /************************************************************************************************************************
     * PRIVATE HELPER METHORDS BELOW ONLY.
     ************************************************************************************************************************/

    /**
     * Verify that an active service is up with the same name.
    * @param name - Service name.
    */
    private void serviceVerificiation(@NotNull String name) {
        // Validation check for service name uniqueness
        List<Service> nameCheck = userServicesService.getServiceByName(name);
        if (nameCheck.size() > 0) {
            for (Service validateService : nameCheck) {
                // Only fail if the named service is Active or Pending
                if (validateService.getStatus().equals(Constants.SERVICE_STATUS_ACTIVE)
                        || validateService.getStatus().equals(Constants.SERVICE_STATUS_PENDING)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Resource already exists with name " + name);
                }
            }
        }
    }


}
