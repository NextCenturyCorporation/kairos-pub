package com.ncc.kairos.moirai.zeus.runner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessRunner runs bash commands and is extended by @TerraformRunner
 * and @AnsibleRunner.
 * 
 * @author Lion Tamer
 */
@Service
public class ProcessRunner {

    @Value("${java.cli.wait.timeout}")
    private Integer waitTimeout;

    private static final Logger LOGGER = Logger.getLogger(ProcessRunner.class.getName());

    public Process runCommands(List<String> commands, String workingDirectory) throws ResponseStatusException {
        LOGGER.log(Level.INFO, "Directory: " + workingDirectory);

        StringBuilder commandBlock = new StringBuilder();
        for (String command : commands) {
            commandBlock.append(command + "; ");
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(workingDirectory));

        // Run the Process command block
        processBuilder.command("sh", "-c", commandBlock.toString());
        LOGGER.log(Level.INFO, () -> "************** Launching COMMAND:     " + processBuilder.command().toString());
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();

            // Captures and log the output of the process with line numbers
            InputStream inputReader = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputReader));
            String readline;
            while ((readline = reader.readLine()) != null) {
                if (readline.contains("ERROR 409") || readline.contains("409 CONFLICT")) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Processing commands failed Docker registry already exists.");        
                }
                LOGGER.log(Level.INFO, readline);
            }

            process.waitFor(this.waitTimeout, TimeUnit.MINUTES);
            // Close resources
            inputReader.close();
            reader.close();

            // Destroy the process
            process.destroy();
            process.waitFor(3, TimeUnit.SECONDS);
            LOGGER.log(Level.INFO, () -> "Finished executing Command --> Exit Code: " + process.exitValue());

            return process;
        // Catch all exception and handle it as a ResponseStatusException to report to front end.
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Process was unable to run for Command: \n" + commandBlock.toString());
        }
    }

    public String runCommandOutput(List<String> commands, String workingDirectory)
            throws ResponseStatusException {
                LOGGER.log(Level.INFO, "Directory: " + workingDirectory);
        StringBuilder commandBlock = new StringBuilder();
        for (String command : commands) {
            commandBlock.append(command + "; ");
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(workingDirectory));

        // Run the Process command block
        processBuilder.command("sh", "-c", commandBlock.toString());
        LOGGER.log(Level.INFO, "************** Launching COMMAND:     " + processBuilder.command().toString());
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();

            // Captures and log the output of the process with line numbers
            InputStream inputReader = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputReader));
            String output = "";
            String readline;
            while ((readline = reader.readLine()) != null) {
                if (readline.contains("ERROR 409") || readline.contains("409 CONFLICT")) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Processing commands failed Docker registry already exists.");        
                }
                output += readline;
                LOGGER.log(Level.INFO, readline);
            }

            process.waitFor(this.waitTimeout, TimeUnit.MINUTES);
            // Close resources
            inputReader.close();
            reader.close();

            // Destroy the process
            process.destroy();
            process.waitFor(3, TimeUnit.SECONDS);
            LOGGER.log(Level.INFO, "Finished executing Command --> Exit Code: " + process.exitValue());

            return output;
        // Catch all exception and handle it as a ResponseStatusException to report to front end.
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Process was unable to run for Command: \n" + commandBlock.toString());
        }
    }


}
