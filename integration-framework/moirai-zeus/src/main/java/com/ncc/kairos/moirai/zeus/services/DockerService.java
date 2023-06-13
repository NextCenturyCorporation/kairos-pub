package com.ncc.kairos.moirai.zeus.services;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.ncc.kairos.moirai.zeus.dao.DockerRegistryRepository;
import com.ncc.kairos.moirai.zeus.dao.DockerUploadRepository;
import com.ncc.kairos.moirai.zeus.model.*;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;
import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * User Service layer to handle business logic for Docker endpoints.
 *
 * @author Lion Tamer
 */
@org.springframework.stereotype.Service
public class DockerService {

    private static final Logger LOGGER = Logger.getLogger(DockerService.class.getName());

    @Autowired
    private AWSS3Connector awsS3Connector;

    @Autowired
    ProcessRunner processRunner;

    @Autowired
    KairosUserService kairosUserService;

    @Autowired
    private DockerRegistryRepository dockerRegistryRepository;

    @Autowired
    private DockerUploadRepository dockerUploadRepository;


    private void flaskCreateDockerRegistry(String name, String secret) {
        List<String> commands = new ArrayList<>();
        commands.add("wget \"" + Constants.DOCKER_REGISTRY_FLASK_CNAME + Constants.DOCKER_REGISTRY_CREATE_REGISTRY_EP +
                "?performer=" + name + "&secret=" + secret + "\"");
        Process returnedProcess = this.processRunner.runCommands(commands, ".");

        if (returnedProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Processing commands failed to create Docker Registry.");
        }
    }

    private void flaskResetDockerRegistry(String name, String secret) {
        List<String> commands = new ArrayList<>();
        commands.add("wget \"" + Constants.DOCKER_REGISTRY_FLASK_CNAME + Constants.DOCKER_REGISTRY_PASSWORD_RESET_EP +
                "?performer=" + name + "&secret=" + secret + "\"");
        Process returnedProcess = this.processRunner.runCommands(commands, ".");

        if (returnedProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Processing commands failed to reset Docker Registry password.");
        }
    }

    private DockerRegistry saveNewRegistry(String owner, String password) {
        return this.dockerRegistryRepository.save(
                new DockerRegistry()
                        .owner(owner.toLowerCase())
                        .endpoint("https://" + owner.toLowerCase() + "." + Constants.DOCKER_REGISTRY_CNAME)
                        .password(password));
    }

    public void createDockerRegistry(String secret, String teamName) {
        this.flaskCreateDockerRegistry(teamName, secret);
        this.saveNewRegistry(teamName, PasswordUtil.encode(secret));
    }

	public void resetDockerPassword(String value, String teamName) {
        DockerRegistry registry = this.dockerRegistryRepository.findByOwner(teamName);
        this.flaskResetDockerRegistry(teamName, value);

        this.dockerRegistryRepository.save(registry.password(PasswordUtil.encode(value)));
    }

	public List<DockerRegistry> getDockerRegistries(String value) {
        List<DockerRegistry> found = new ArrayList<>();
        if (value.equals("*")) {
            found = this.dockerRegistryRepository.findAll();
        } else {
            found.add(this.dockerRegistryRepository.findByOwner(value));
        }
        if (found == null || found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Results returned");
        }
        return found;
	}

	public void deleteDockerUpload(String id, String teamName) {
        try {
            DockerUpload uploadToDelete = this.dockerUploadRepository.findById(id).get();
            DockerRegistry currentRegistry = this.dockerRegistryRepository.findByOwner(teamName);
            
            if (uploadToDelete == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error finding upload with id " + id);
            }
            if (currentRegistry == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error finding registry for " + teamName);
            }

            List<DockerUpload> updatedList = currentRegistry.getDockerimagelist();
            updatedList.remove(uploadToDelete);
            currentRegistry.setDockerimagelist(updatedList);
            this.dockerRegistryRepository.save(currentRegistry);
            this.dockerUploadRepository.delete(uploadToDelete);

            awsS3Connector.deleteDockerUpload(uploadToDelete);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error finding registry");
        }
    }

    public void discoverRegistries() {
        List<S3ObjectSummary> summaries = this.awsS3Connector.listFileSummaries(Constants.DOCKER_REGISTRY_BUCKET_NAME, Constants.REPO_FOLDER);
        List<String> repos = summaries.stream().map(summary -> awsS3Connector.getRegistryFromKey(summary.getKey()).toLowerCase()).distinct().collect(Collectors.toList());
        repos.forEach(repo -> {
            DockerRegistry existingRegistry = this.dockerRegistryRepository.findByOwner(repo);
            if (existingRegistry == null) {
                this.saveNewRegistry(repo, null);
            }
        });
    }

    public void refreshAllRegistries() {
        List<DockerRegistry> registries = this.dockerRegistryRepository.findAll();
        registries.forEach(registry -> refreshRegistry(registry));
    }

    public void refreshRegistry(String teamName) {
        DockerRegistry registry = this.dockerRegistryRepository.findByOwner(teamName);
        refreshRegistry(registry);
    }

    public void refreshRegistry(DockerRegistry registry) {
        if (registry != null) {
            List<DockerUpload> newUploads = this.awsS3Connector.getDockerRegistryS3Data(registry.getOwner().toLowerCase());
            // For each upload found in s3 see if a record already exists.
            List<DockerUpload> uploadsToSave = newUploads.stream().map(upload -> {
                List<DockerUpload> existingMatches = registry.getDockerimagelist().stream()
                        .filter(record -> (record.getRepo().equals(upload.getRepo()) && record.getDigest().equals(upload.getDigest())))
                        .collect(Collectors.toList());
                if (existingMatches.size() == 1) {
                    upload.setId(existingMatches.get(0).getId());
                }
                return this.dockerUploadRepository.save(upload);
            }).collect(Collectors.toList());
            this.dockerRegistryRepository.save(registry.dockerimagelist(uploadsToSave));

            // For every record existing see if the upload is still in s3
            List<String> idsToRetain = uploadsToSave.stream().map(upload -> upload.getId()).filter(id -> !id.isEmpty()).collect(Collectors.toList());
            List<DockerUpload> uploadsToRemove = dockerUploadRepository.findByRegistry(registry.getOwner()).stream().filter(record -> !idsToRetain.contains(record.getId())).collect(Collectors.toList());
            uploadsToRemove.forEach(upload -> this.dockerUploadRepository.delete(upload));
        }
    }

    public void restoreAllRegistries() {
        List<DockerRegistry> registries = this.dockerRegistryRepository.findAll();
        List<DockerRegistry> failedRegistries = new ArrayList<>();
        for (DockerRegistry registry : registries) {
            try {
                if (registry.getPassword() != null) {
                    this.flaskCreateDockerRegistry(registry.getOwner(), PasswordUtil.decode(registry.getPassword()));
                }
            } catch (Exception e) {
                failedRegistries.add(registry);
            }
        }
        if (failedRegistries.size() > 0) {
            failedRegistries.forEach(r -> LOGGER.warning("Failed to restore registries: " + r.getEndpoint()));
        }
    }
}
