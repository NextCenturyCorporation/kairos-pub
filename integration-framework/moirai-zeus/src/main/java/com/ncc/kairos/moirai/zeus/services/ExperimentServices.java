package com.ncc.kairos.moirai.zeus.services;

import java.io.File;
import java.io.FileWriter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.json.JSONArray;
//import org.springframework.scheduling.annotation.Scheduled;
import org.json.JSONObject;

import com.ncc.kairos.moirai.zeus.dao.ExperimentRepository;
import com.ncc.kairos.moirai.zeus.dao.ExperimentRunRepository;
import com.ncc.kairos.moirai.zeus.model.Experiment;
import com.ncc.kairos.moirai.zeus.model.ExperimentRun;
import com.ncc.kairos.moirai.zeus.model.NodeTypeResponse;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.resources.ExperimentRunStatus;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.runner.TerraformRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSEC2Connector;

@Service
@Transactional
public class ExperimentServices {

    private static final Logger LOGGER = Logger.getLogger(ExperimentServices.class.getName());

    @Autowired
    ExperimentRepository experimentRepository;

    @Autowired
    ExperimentRunRepository experimentRunRepository;

    @Autowired
    AWSEC2Connector connector;

    @Autowired
    ProcessRunner processRunner;

    @Autowired
    TerraformRunner terraformRunner;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
    }

    public List<Experiment> getAllExperiments() {
        return this.experimentRepository.findAll();
    }

    public void saveOrUpdateExperiment(@Valid Experiment experiment) {
        // Updating Experiment
        if (!experiment.getId().isEmpty()) {
            Optional<Experiment> stored = this.experimentRepository.findById(experiment.getId());
            if (!stored.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No Experiment found for ID: " + experiment.getId());
            }
        }
        // save existing or new
        this.experimentRepository.save(experiment);
    }

    public void deleteExperiment(@Valid String id) {
        Optional<Experiment> stored = this.experimentRepository.findById(id);
        if (!stored.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Experiment found for ID: " + id);
        }
        this.experimentRepository.deleteById(id);
    }

    public NodeTypeResponse getNodeTypes() {
        NodeTypeResponse response = new NodeTypeResponse();

        List<String> cpu = new ArrayList<>();
        cpu.add("m5.*");
        cpu.add("m5a.*");
        response.setCpuTypes(connector.getInstanceTypes(cpu));

        List<String> gpu = new ArrayList<>();
        gpu.add("p3.*");
        response.setGpuTypes(connector.getInstanceTypes(gpu));

        return response;
    }

    public String getK8sStatus() {
        try {
            List<String> commands = new ArrayList<>();
            commands.add(
                    "eksName=$(aws eks describe-cluster --name hippodrome --region us-east-1 | jq -r '.cluster.name')");
            commands.add(
                    "eksArn=$(aws eks describe-cluster --name hippodrome --region us-east-1 | jq -r '.cluster.arn')");
            commands.add("kubectl config unset contexts.arn:$eksArn");
            commands.add("aws eks update-kubeconfig --name $eksName --region us-east-1");
            commands.add("kubectl rollout status statefulset redis-cluster -n kairos-redis");
            String value = this.processRunner.runCommandOutput(commands, ".");
            if (value.contains("partitioned roll out complete")) {
                return Constants.EXPERIMENT_K8S_ENV_ACTIVE;
            } else if (value.contains("Cluster status is CREATING")) {
                return Constants.EXPERIMENT_K8S_ENV_PENDING;
            }
        } catch (Exception e) {
            return Constants.EXPERIMENT_K8S_ENV_TERMINATED;
        }
        return Constants.EXPERIMENT_K8S_ENV_TERMINATED;
    }

    public void createExperimentRun(String experimentId) {
        // get experiment from back end
        Optional<Experiment> optExperiment = this.experimentRepository.findById(experimentId);
        if (!optExperiment.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Experiment found for ID: " + experimentId);
        }
        Experiment experiment = optExperiment.get();

        // create new ExperimentRun and save to experiment run repo
        ExperimentRun experimentRun = new ExperimentRun();
        experimentRun.setName(experiment.getName());
        experimentRun.setStartTime(OffsetDateTime.now());
        experimentRun.setStatus(ExperimentRunStatus.REQUESTED.name());
        experimentRun.setAttempt(0);

        // add start time to experimentRun config
        JSONObject config = new JSONObject(experiment.getValue());
        JSONObject experimentConfig = config.getJSONObject("experiment");

        experimentConfig.put("startTime", OffsetDateTime.now());
        config.put("experiment", experimentConfig);
        experimentRun.setConfig(config.toString());
        
        this.experimentRunRepository.save(experimentRun);

        // add run to experiment and save experiment
        experiment.addExperimentRunsItem(experimentRun);
        this.experimentRepository.save(experiment);
    }

    public void provisionNodeGroups(ExperimentRun run) throws Exception {
        // Running terraform init
        HashMap<String, String> backendConfigs = new HashMap<>();
        backendConfigs.put("key", "experiment-perfeval-" + run.getName());
        Process value = this.terraformRunner.terraformInit(backendConfigs,
                "experiment-runtime/experiment-perfeval-nodes");

        // Need to read config file from run (JSON)
        // probably a better way to do this but good enough for now
        JSONObject runConfig = new JSONObject(run.getConfig());
        JSONObject cpu = runConfig.getJSONObject("cluster").getJSONObject("cpu");
        JSONObject gpu = runConfig.getJSONObject("cluster").getJSONObject("gpu");


        HashMap<String, String> configVars = new HashMap<>();
        configVars.put("experiment", run.getName());
        configVars.put("cpu_nodesmin", Integer.toString(cpu.getInt("desired")));
        configVars.put("cpu_nodetype", cpu.getString("type"));
        configVars.put("gpu_nodesmin", Integer.toString(gpu.getInt("desired")));
        configVars.put("gpu_nodetype", cpu.getString("type"));

        // Running terraform apply
        value = this.terraformRunner.runTerraform("experiment-runtime/experiment-perfeval-nodes", configVars,
                "apply --auto-approve");

        // on failure
        if (value.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Issue creating WorkLoads for K8s.");
        }
    }

    /**
     * Add control finger print taken from zhuli_dothething.sh.
     */
    public void addControlFingerprint() throws Exception {
        List<String> commands = new ArrayList<>();
        commands.add("touch ~/.ssh/known_hosts");
        commands.add("ssh-keygen -R control.hippodrome.kairos.nextcentury.com");
        commands.add("ssh-keyscan -H control.hippodrome.kairos.nextcentury.com >> ~/.ssh/known_hosts");

        Process value = this.processRunner.runCommands(commands, Constants.TERRAFORM_FOLDER_LOCATION + "/experiment-runtime/");
        if (value.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Issue creating WorkLoads for K8s.");
        }
    }

    public void provisionEnclave(ExperimentRun run) throws Exception {
        File tmpFile = File.createTempFile(run.getName(), ".json", new File(Constants.TERRAFORM_FOLDER_LOCATION + "/experiment-runtime"));
        FileWriter writer = new FileWriter(tmpFile);

        writer.write(run.getConfig());
        writer.close();

        List<String> commands = new ArrayList<>();

        commands.add("scp -i " + Constants.PEM_FILE + " " + tmpFile.getAbsolutePath()
                + " ec2-user@control.hippodrome.kairos.nextcentury.com:/home/ec2-user/experiment-config/"
                + run.getName() + ".json");

        commands.add("ssh -i " + Constants.PEM_FILE
                + " ec2-user@control.hippodrome.kairos.nextcentury.com /home/ec2-user/scripts/create-enclave.sh -cf /home/ec2-user/experiment-config/"
                + run.getName() + ".json");

        // run commands
        Process value = this.processRunner.runCommands(commands,
                Constants.TERRAFORM_FOLDER_LOCATION + "/experiment-runtime/");
        // check response
        tmpFile.delete();
        if (value.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Issue creating WorkLoads for K8s.");
        }
    }

    public void runEnclave(ExperimentRun run) {
        List<String> commands = new ArrayList<>();

        commands.add("ssh -i " + Constants.PEM_FILE
                + " ec2-user@control.hippodrome.kairos.nextcentury.com /home/ec2-user/scripts/run-enclave.sh -cf /home/ec2-user/experiment-config/"
                + run.getName() + ".json");

        Process value = this.processRunner.runCommands(commands,
                Constants.TERRAFORM_FOLDER_LOCATION + "/experiment-runtime/");

        if (value.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Issue creating WorkLoads for K8s.");
        }
    }

    public void watchEnclave(ExperimentRun run, JSONArray experimentArray) {
        // JSONArray experimentArray = experiments.toJSONArray(experiments.names());

        // for each does not play nice with JSONArray
        for (int i = 0; i < experimentArray.length(); i++) {
            JSONObject currentExperiment = experimentArray.getJSONObject(i);

            // find corresponding experiment
            if (run.getName().equals(currentExperiment.getString("experiment"))) {
                String age = currentExperiment.getString("age");
                // check if last letter in age is s (seconds)
                if (!age.substring(age.length() - 1).equals("s")) {
                    // check if greater than 30 minutes
                    if (!age.substring(age.length() - 1).equals("m")
                            || Integer.parseInt(age.substring(0, 2)) > 30) {
                        // if status is "waiting" set status to saving
                        if (currentExperiment.getString("status").equals("WAITING")) {
                            run.setStatus(ExperimentRunStatus.SAVING.name());
                            this.experimentRunRepository.save(run);
                        }
                    }
                }
            }
        }
    }

    public void destoryEnclave(ExperimentRun run) {
        run.setStatus(ExperimentRunStatus.DESTROYING_ENCLAVE.name());
        this.experimentRunRepository.save(run);

        String sshPrefix = "ssh -i " + Constants.PEM_FILE + " ec2-user@control.hippodrome.kairos.nextcentury.com ";
        
        String cleanDirCommand = "sudo rm -rf /var/kairosfs/" + run.getName();
        String cleanDirTest = "ls -al /var/kairosfs/" + run.getName();
        String cleanNamespaceCommand = "kubectl delete namespace c-" + run.getName().toLowerCase() + "-enclave";
        String cleanNamespaceTest = "kubectl get namespace c-" + run.getName().toLowerCase() + "-enclave";
        
        //If the directory or namespace doesnt exist prior to running this command fails, thats okay
        this.processRunner.runCommands(Collections.singletonList(sshPrefix + cleanDirCommand), Constants.TERRAFORM_FOLDER_LOCATION + "experiment-runtime");
        this.processRunner.runCommands(Collections.singletonList(sshPrefix + cleanNamespaceCommand), Constants.TERRAFORM_FOLDER_LOCATION + "experiment-runtime");

        // What matters if that that no longer exist at this point, We WANT these to fail
        Process cleanDirValue = this.processRunner.runCommands(Collections.singletonList(sshPrefix + cleanDirTest), Constants.TERRAFORM_FOLDER_LOCATION + "experiment-runtime");
        Process cleanNamespaceValue = this.processRunner.runCommands(Collections.singletonList(sshPrefix + cleanNamespaceTest), Constants.TERRAFORM_FOLDER_LOCATION + "experiment-runtime");
        // check response
        if (cleanDirValue.exitValue() == 0 || cleanNamespaceValue.exitValue() == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Issue destroying enclave for " + run.getName() + ".");
        }
    }

    public void destroyNodeGroups(ExperimentRun run) {
        run.setStatus(ExperimentRunStatus.DESTROYING_NODEGROUPS.name());
        this.experimentRunRepository.save(run);

        HashMap<String, String> backendConfigs = new HashMap<>();
        backendConfigs.put("key", "experiment-perfeval-" + run.getName());
        Process value = this.terraformRunner.terraformInit(backendConfigs,
                "experiment-runtime/experiment-perfeval-nodes");
                
        // Running terraform apply
        value = this.terraformRunner.runTerraform("experiment-runtime/experiment-perfeval-nodes", new HashMap<>(),
                "destroy --auto-approve");

        // on failure
        if (value.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Issue destroying node group for " + run.getName() + ".");
        }
    }

    public void createNodeGroups(String experimentId, int retryAttempts) {
        if (!getK8sStatus().equals(Constants.EXPERIMENT_K8S_ENV_ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Environment is not up to run experiment" + experimentId);
        }
        // retrieve experiment from backend
        Optional<Experiment> optStored = this.experimentRepository.findById(experimentId);
        if (!optStored.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Experiment found for ID: " + experimentId);
        }
        Experiment stored = optStored.get();

        File tmpFile = null;

        try {
            LOGGER.log(Level.INFO,
                    "Experiment rds-Id: " + stored.getId() + "\nExperiment Configuration: \n" + stored.getValue());
            tmpFile = File.createTempFile(stored.getName(), ".json", new File("/usr/src"));
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(stored.getValue());
            writer.close();

            LOGGER.log(Level.INFO, "Creating temp file: " + tmpFile.getAbsolutePath());

            List<String> commands = new ArrayList<>();
            commands.add("chmod +x *.sh");
            commands.add("./zhuli_dothething.sh -cf " + tmpFile.getAbsolutePath());

            OffsetDateTime startTime = OffsetDateTime.now();
            Process value = this.processRunner.runCommands(commands,
                    Constants.TERRAFORM_FOLDER_LOCATION + "/experiment-runtime/");
            if (value.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: Issue creating WorkLoads for K8s.");
            }
            // Save RunTime configuration if we are successful. Otherwise it was not a run
            // and I don't care
            stored.addExperimentRunsItem(new ExperimentRun()
                    .id(UUID.randomUUID().toString())
                    .name(stored.getName())
                    .startTime(startTime));
            experimentRepository.save(stored);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning("Could not create node group, Tearing down");
            terminateNodeGroups(experimentId, 0);
            if (this.retryEligible("Creating Node groups: " + stored.getName(), retryAttempts) > 0) {
                createNodeGroups(experimentId, --retryAttempts);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to write experiment to file");
            }
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    public void terminateNodeGroups(@NotNull String experimentId, int retryAttempts) {
        if (getK8sStatus().equals(Constants.EXPERIMENT_K8S_ENV_ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Environment is not up to stop experiment" + experimentId);
        }
        // retrieve experiment from backend
        Optional<Experiment> stored = this.experimentRepository.findById(experimentId);
        if (!stored.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Experiment found for ID: " + experimentId);
        }
        File tmpFile = null;
        try {

            tmpFile = File.createTempFile(stored.get().getName(), ".json", new File("/usr/src"));
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(stored.get().getValue());
            writer.close();

            LOGGER.log(Level.INFO, "Creating temp file: " + tmpFile.getAbsolutePath());
            List<String> commands = new ArrayList<>();
            commands.add("chmod +x *.sh");
            commands.add("./therearenomorethingstodo.sh -cf " + tmpFile.getAbsolutePath());

            Process value = this.processRunner.runCommands(commands,
                    Constants.TERRAFORM_FOLDER_LOCATION + "experiment-runtime/");
            if (value.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: Issue creating WorkLoads for K8s.");
            }
            // Set Complete time for runtime, Only 1 runtime should be run per experiment
            // configuration
            for (int i = 0; i < stored.get().getExperimentRuns().size(); i++) {
                if (stored.get().getExperimentRuns().get(i).getCompleteTime() == null) {
                    stored.get().getExperimentRuns().get(i).setCompleteTime(OffsetDateTime.now());
                    experimentRepository.save(stored.get());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (this.retryEligible("Terminating Node groups: " + stored.get().getName(), retryAttempts) > 0) {
                terminateNodeGroups(experimentId, --retryAttempts);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to write experiment to file");
            }
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    // Can only do this when no experiments are running
    public void terminateK8sEnv(int retryAttempts) {
        if (!checkForActiveNodeGroups()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: cannot destroy K8s Environment when experiments are active.");
        }
        try {
            this.terraformRunner.terraformInit(new HashMap<>(), "experiment-runtime/experiment-perfeval-framework");
            Process terraformProcess = this.terraformRunner.runTerraform(
                    "experiment-runtime/experiment-perfeval-framework", new HashMap<>(), "destroy --auto-approve");

            this.terraformRunner.terraformInit(new HashMap<>(), "experiment-runtime/experiment-perfeval");
            terraformProcess = this.terraformRunner.runTerraform("experiment-runtime/experiment-perfeval",
                    new HashMap<>(), "destroy --auto-approve");
            if (terraformProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: cannot destroy k8s perfeval.");
            }

            List<String> removeCloudWatchlogs = Arrays.asList(
                    "aws logs delete-log-group --log-group-name /aws/eks/hippodrome/cluster || echo no group found");
            this.processRunner.runCommands(removeCloudWatchlogs, ".");

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: running terraform process. terminateK8sEnv");
        }
    }

    public void createK8sEnv(int retryAttempts) {
        try {
            // experiment-env
            this.terraformRunner.terraformInit(new HashMap<>(), "experiment-runtime/experiment-env");
            Process terraformProcess = this.terraformRunner.runTerraform("experiment-runtime/experiment-env",
                    new HashMap<>(), "apply --auto-approve");
            if (terraformProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: cannot create k8s env.");
            }

            this.terraformRunner.terraformInit(new HashMap<>(), "experiment-runtime/experiment-perfeval");
            terraformProcess = this.terraformRunner.runTerraform("experiment-runtime/experiment-perfeval",
                    new HashMap<>(), "apply --auto-approve");
            if (terraformProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: cannot create k8s perfeval.");
            }

            this.terraformRunner.terraformInit(new HashMap<>(), "experiment-runtime/experiment-perfeval-framework");
            terraformProcess = this.terraformRunner.runTerraform("experiment-runtime/experiment-perfeval-framework",
                    new HashMap<>(), "apply --auto-approve");
            if (terraformProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: cannot create k8s perfeval framework.");
            }

        } catch (Exception e) {
            LOGGER.info("Error Creating K8s ENV, Tearing Down");
            terminateK8sEnv(0);
            if (this.retryEligible("Create K8s Env.", retryAttempts) > 0) {
                createK8sEnv(--retryAttempts);
            } else {
                LOGGER.warning("Could not create K8s environment, Tearing down");
                throw e;
            }
        }
    }

    /************************************************************************************************************************
     * PRIVATE HELPER METHORDS BELOW ONLY.
     ************************************************************************************************************************/
    private boolean checkForActiveNodeGroups() {
        try {
            List<String> commands = new ArrayList<>();
            commands.add(
                    "eksName=$(aws eks describe-cluster --name hippodrome --region us-east-1 | jq -r '.cluster.name')");
            commands.add("eksctl get nodegroup --cluster=$eksName");
            String value = this.processRunner.runCommandOutput(commands, ".");
            if (value.contains("frameworkng") || value.contains("workerng")) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private int retryEligible(String service, int retryAttempt) {
        if (retryAttempt > 0) {
            LOGGER.info(
                    "****************************************************************************************************");
            LOGGER.info("    Attempting Retries left- " + --retryAttempt + ": " + service);
            LOGGER.info(
                    "****************************************************************************************************");
            return retryAttempt;
        }
        return -1;
    }
}
