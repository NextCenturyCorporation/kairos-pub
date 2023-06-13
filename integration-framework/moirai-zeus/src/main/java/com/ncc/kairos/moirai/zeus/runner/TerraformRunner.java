package com.ncc.kairos.moirai.zeus.runner;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ncc.kairos.moirai.zeus.resources.Constants;

/**
 * Terraform Runner class to run terrafrom scripts within /usr/src/terraform directory.
 * @author Lion Tamer
 */
@Service
public class TerraformRunner extends ProcessRunner {

    // Run the TerraformProject
    public Process runTerraform(String scriptName, Map<String, String> extraVars, String applyDestroy) throws ResponseStatusException {
        return runTerraform(scriptName, extraVars, applyDestroy, Constants.TERRAFORM_FOLDER_LOCATION);
    }

    // Run the TerraformProject
    public Process runTerraform(String scriptName, Map<String, String> extraVars, String applyDestroy, String path) throws ResponseStatusException {
        String commandString = String.format("terraform %s", applyDestroy);
        for (Map.Entry<String, String> entry : extraVars.entrySet()) {
            commandString += " -var=\"" + entry.getKey() + "=" + entry.getValue() + "\" ";
        }
        return runCommands(Collections.singletonList(commandString), path + scriptName);
    }

    /**
     * Terraform init designed for backend configuration changes.
     * @param backendConfigs Map of key value pairs to add to terraforms backend configurations.
     * @param projectFolder Project source folder.
     * @return Process object.
     */
    public Process terraformInit(Map<String, String> backendConfigs, String projectFolder) {
        List<String> commands = new ArrayList<>();
        String commandString = String.format("terraform init ");
        for (Map.Entry<String, String> entry : backendConfigs.entrySet()) {
            commandString += " -backend-config=\"" + entry.getKey() + "=" + entry.getValue() + "\" -reconfigure ";
        }
        commands.add(commandString);
        return runCommands(commands, Constants.TERRAFORM_FOLDER_LOCATION + projectFolder);
    }

}
