package com.ncc.kairos.moirai.zeus.runner;

import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.resources.EnvironmentTier;
import com.ncc.kairos.moirai.zeus.services.PropertiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ansible Runner class to run ansible playbooks within /usr/src/ansible directory.
 * Handles extra-vars to be passed to the ansible scripts.
 *
 * @author Lion Tamer
 */
@Service
public class AnsibleRunner extends ProcessRunner {

    private static final Logger LOGGER = Logger.getLogger(AnsibleRunner.class.getName());

    @Autowired
    PropertiesService propertiesService;

    /**
     * Run the Ansible EC2 Terminate playbook.
     *
     * @param scriptName - playbook name
     * @param extraVars  - Extra variables for playbook
     * @param verbosity  - Level of logging
     * @return @Process for exit code and errors
     * @throws ResponseStatusException - exception
     */
    public Process runAnsible(String scriptName, Map<String, String> extraVars, int verbosity) throws ResponseStatusException {
        String formattedExtraVars = formatExtraVars(extraVars);
        String formattedVerbosity = formatVerbosity(verbosity);

        String workingDirectory = Constants.ANSIBLE_FOLDER_LOCATION;
        String commandString = String.format("ansible-playbook %s -i inventory %s %s", scriptName, formattedExtraVars, formattedVerbosity);
        LOGGER.log(Level.INFO, commandString);
        if (propertiesService.whichEnvironment() == EnvironmentTier.DEVELOPMENT) {
            // If we are local we don't have ansible so instead we just echo the command we would have run
            commandString = "echo '" + commandString + "'";
            workingDirectory = "/";
        }
        return runCommands(Collections.singletonList(commandString), workingDirectory);
    }

    /**
     * Takes a map and returns a string of '--extra-vars "key1=value1 key2=value2"'.
     * An empty map just returns an empty string
     */
    private String formatExtraVars(Map<String, String> extraVars) {
        String extraVarString = "";
        if (extraVars != null && !extraVars.isEmpty()) {
            extraVarString += "--extra-vars \"";
            for (Map.Entry<String, String> entry : extraVars.entrySet()) {
                extraVarString += String.format(" %s=%s", entry.getKey(), entry.getValue());
                LOGGER.log(Level.INFO, extraVarString);
            }
            extraVarString += "\"";
        }
        LOGGER.log(Level.INFO, extraVarString);
        return extraVarString;
    }

    /**
     * Formats level of verbosity.
     *
     * @param verbosity - integer for how many v's (verbosity)
     * @return
     */
    private String formatVerbosity(int verbosity) {
        String returnVal = verbosity > 0 ? "-" : "";
        while (verbosity > 0) {
            returnVal += "v";
            verbosity--;
        }
        return returnVal;
    }

}
