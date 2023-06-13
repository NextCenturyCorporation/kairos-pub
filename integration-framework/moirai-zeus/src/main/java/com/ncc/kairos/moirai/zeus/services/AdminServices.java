package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.DockerRegistryRepository;
import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.dao.StoredFileRepository;
import com.ncc.kairos.moirai.zeus.model.DockerRegistry;
import com.ncc.kairos.moirai.zeus.model.GitLabRequest;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.Service;
import com.ncc.kairos.moirai.zeus.model.StoredFile;
import com.ncc.kairos.moirai.zeus.model.UserDataDto;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.runner.AnsibleRunner;
import com.ncc.kairos.moirai.zeus.runner.ProcessRunner;
import com.ncc.kairos.moirai.zeus.runner.TerraformRunner;
import com.ncc.kairos.moirai.zeus.utililty.AWSEC2Connector;
import com.ncc.kairos.moirai.zeus.utililty.AWSS3Connector;
import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

/**
 * Admin Service layer to handle business logic for Admininstrative endpoints.
 *
 * @author Lion Tamer
 */
@org.springframework.stereotype.Service
public class AdminServices {

    @Autowired
    private JwtRoleRepository jwtRoleRepository;

    @Autowired
    private KairosUserService kairosUserService;

    @Autowired
    TerraformRunner terraformRunner;

    @Autowired
    AnsibleRunner ansibleRunner;

    @Autowired
    ProcessRunner processRunner;

    @Autowired
    StoredFileRepository storeFileRepo;

    @Autowired
    DockerRegistryRepository dockerRegistryRepo;

    @Autowired
    DockerService dockerService;

    @Autowired
    AWSS3Connector awsS3Connector;

    @Autowired
    TAServices taServices;

    @Autowired
    UserServicesService userServicesService;

    @Autowired
    AWSEC2Connector eConnector;

    @Value("${environment}")
    private String environment;

    public void provisionGitlab(GitLabRequest gitLabRequest) {
        // TODO create and add terraform extra vars converter that takes a gitLabRequest
        // and returns a map of extra vars
        Map<String, String> extraVars = new HashMap<>();
        this.terraformRunner.runTerraform("gitlab-tf", extraVars, "init");
        Process terraformProcess = this.terraformRunner.runTerraform("gitlab-tf", extraVars, "apply --auto-approve");
        if (terraformProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Error running terraform");
        } else {
            Process ansibleProcess = this.ansibleRunner.runAnsible("gitlab-runner-init.yml", extraVars, 2);
            if (ansibleProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Error running ansible");
            }
        }
    }

    public void destroyGitlab() {
        Map<String, String> extraVars = new HashMap<>();
        Process ansibleProcess = this.ansibleRunner.runAnsible("gitlab-backup.yml", extraVars, 2);
        if (ansibleProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Error running ansible");
        } else {
            this.terraformRunner.runTerraform("gitlab-tf", extraVars, "init");
            Process terraformProcess = this.terraformRunner.runTerraform("gitlab-tf", extraVars,
                    "destroy --auto-approve");
            if (terraformProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error: Error running terraform");
            }
        }
    }

    /**
     * Provisions the Infrastructure needed for enclaves.
     */
    public void provisionEnclaveInfrastructure() {
        List<String> commands = new ArrayList<>();
        commands.add("./create-infrastructure.sh -vpcid " + eConnector.getVpcId() + " -pgm Moirai -env "
                + this.environment);
        Process returnedProcess = this.processRunner.runCommands(commands, "/usr/src/k8s");
        if (returnedProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Processing commands failed to create infrastructure");
        }
    }

    public void destroyEnclaveInfrastructure() {
        List<String> commands = new ArrayList<>();
        commands.add("./delete-infrastructure.sh -pgm Moirai -env " + this.environment);
        Process returnedProcess = this.processRunner.runCommands(commands, "/usr/src/k8s");
        if (returnedProcess.exitValue() != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Processing commands failed to destroy infrastructure");
        }
    }

    public void createOrUpdateRoles(List<JwtRole> roleArray) {
        if (!roleArray.isEmpty()) {
            for (JwtRole role : roleArray) {
                if (role.getPermissions().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Role has no permissions.");
                }
                // Updating a role
                if (!role.getId().isEmpty()) {
                    Optional<JwtRole> storedRole = this.jwtRoleRepository.findById(role.getId());
                    if (!storedRole.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Invalid ID.");
                    }
                    JwtRole optional = storedRole.get();
                    optional.permissions(role.getPermissions());
                    optional.description(role.getDescription());
                    this.jwtRoleRepository.save(optional);
                } else {
                    // Creating a new one check name for conflicts
                    JwtRole existingRole = this.jwtRoleRepository.findByName(role.getName());
                    if (existingRole != null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Role already exists.");
                    }
                    this.jwtRoleRepository.save(role);
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warning: No Roles were passed");
        }
    }

    public void deleteRoles(@Valid JwtRole jwtRole) {
        List<JwtUser> userList = this.kairosUserService.findAllUsers();
        Optional<JwtRole> storedRole = this.jwtRoleRepository.findById(jwtRole.getId());
        if (!storedRole.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: No Role was found.");
        }
        if (userList.stream().anyMatch(e -> e.getRoles().contains(storedRole.get()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Error: Role is being used by a user account.");
        }
        this.jwtRoleRepository.delete(storedRole.get());
    }

    public void updateUserAccount(@Valid UserDataDto userData) {
        JwtUser user = this.kairosUserService.findUserById(userData.getId());
        List<JwtRole> rolesToAssign = new ArrayList<>();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        if (StringUtils.isNotEmpty(userData.getEmailAddress())
                && !user.getEmailAddress().equals(userData.getEmailAddress())) {
            this.kairosUserService.assertUniqueEmailAddress(userData.getEmailAddress());
            user.setEmailAddress(userData.getEmailAddress());
        }
        if (StringUtils.isNotEmpty(userData.getPerformerGroup())) {
            user.setPerformerGroup(userData.getPerformerGroup());
        }
        user.active(userData.getActive());

        for (JwtRole role : userData.getRoles()) {
            Optional<JwtRole> storedRole = this.jwtRoleRepository.findById(role.getId());
            if (!storedRole.isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more Permissions does not exist.");
            }
            rolesToAssign.add(storedRole.get());
        }
        user.setRoles(rolesToAssign);
        this.kairosUserService.saveUser(user);
    }

    public void updateTeamName(JwtUser jwtUser, String newTeamName) {
        this.kairosUserService.assertUniqueTeamName(newTeamName);
        // Get full object
        jwtUser = this.kairosUserService.findUserByUsername(jwtUser.getUsername());
        String currentTeamName = jwtUser.getTeamName();

        List<JwtUser> subAccounts = this.kairosUserService.findUserByTeamName(currentTeamName);
        // Update subTeams
        for (JwtUser subAccount : subAccounts) {
            subAccount.teamName(newTeamName);
            this.kairosUserService.saveUser(subAccount);
        }

        // Update services
        List<Service> teamServices = this.userServicesService.getServicesByTeamName(currentTeamName);
        System.out.println(teamServices.toString());
        for (Service teamService : teamServices) {
            teamService.setTeamName(newTeamName);
            this.userServicesService.updateServiceStatus(teamService);
        }

        // Create new Registry if we have one
        DockerRegistry registry = this.dockerRegistryRepo.findByOwner(currentTeamName);
        if (registry != null) {
            jwtUser.setTeamName(newTeamName);
            this.dockerService.createDockerRegistry(PasswordUtil.decode(registry.getPassword()), jwtUser.getTeamName());

            // Copy all uploads to new / existing registry
            this.awsS3Connector.moveDockerUpload(newTeamName.toLowerCase(), currentTeamName.toLowerCase());
            deleteDockerRegistry(currentTeamName);
        }

        List<StoredFile> storedFilesByTeam = this.storeFileRepo.findAllByOwner(currentTeamName);
        // Update all existing ownerships to new teamname
        if (!storedFilesByTeam.isEmpty()) {
            for (StoredFile file : storedFilesByTeam) {
                file.owner(newTeamName);
                this.storeFileRepo.save(file);
            }
        }

        // Updates the teamname for the user
        jwtUser.teamName(newTeamName);
        this.kairosUserService.saveUser(jwtUser);
    }

    public void migrateTeamName(@Valid JwtUser jwtUser, @Valid String teamName) {
        jwtUser = this.kairosUserService.findUserByUsername(jwtUser.getUsername());

        // If registry exists under the old name we need to retain these images
        // If a registry already exists under the new name, fail because we don't know
        // how to merge
        // If there is no registry under the new name, rename the current registry
        DockerRegistry oldRegistry = this.dockerRegistryRepo.findByOwner(jwtUser.getTeamName());
        if (oldRegistry != null) {
            DockerRegistry newRegistry = this.dockerRegistryRepo.findByOwner(teamName);
            if (newRegistry != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Can't move registry " + jwtUser.getTeamName()
                        + " to " + teamName + " because one already exists");
            } else {
                String oldRegistryName = jwtUser.getTeamName();
                jwtUser.setTeamName(teamName);
                // Todo Update CreateDockerRegistry to take the string teamname instead of the
                // jwtUser object
                this.dockerService.createDockerRegistry(PasswordUtil.decode(oldRegistry.getPassword()),
                        jwtUser.getTeamName());
                jwtUser.setTeamName(oldRegistryName);
                this.awsS3Connector.moveDockerUpload(teamName.toLowerCase(), jwtUser.getTeamName().toLowerCase());
                deleteDockerRegistry(jwtUser.getTeamName());
            }
        }

        List<StoredFile> storedFilesByTeam = this.storeFileRepo.findAllByOwner(jwtUser.getTeamName());
        // Update all existing ownerships to new teamname
        if (!storedFilesByTeam.isEmpty()) {
            for (StoredFile file : storedFilesByTeam) {
                file.owner(teamName);
                this.storeFileRepo.save(file);
            }
        }

        List<JwtUser> subAccounts = this.kairosUserService.findUserByTeamName(jwtUser.getTeamName());
        // Update subAccounts
        for (JwtUser subAccount : subAccounts) {
            subAccount.teamName(teamName);
            this.kairosUserService.saveUser(subAccount);
        }

        // Updates the teamname for the user
        jwtUser.teamName(teamName);
        this.kairosUserService.saveUser(jwtUser);
    }

    public void deleteDockerRegistry(@Valid String teamName) {
        DockerRegistry registry = this.dockerRegistryRepo.findByOwner(teamName);
        if (registry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Error: Processing commands failed to delete Docker Registry. No Registry found");
        }
        registry.getDockerimagelist().forEach(dockerUpload -> this.awsS3Connector.deleteDockerUpload(dockerUpload));
        this.dockerRegistryRepo.delete(registry);

        List<String> commands = new ArrayList<>();
        // Flask service call to server to stop container, remove htpasswd file, delete
        // container
        commands.add("wget \"" + Constants.DOCKER_REGISTRY_FLASK_CNAME + Constants.DOCKER_REGISTRY_DELETE_REGISTRY_EP
                + "?performer=" + teamName.toLowerCase() + "\"");
        Process returnedProcess;
        try {
            returnedProcess = this.processRunner.runCommands(commands, ".");
            if (returnedProcess.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error: Processing commands failed to delete Docker Registry.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Something Went wrong in processRuner.");
        }
    }

    public void toggleAccountActivation(@Valid String userName) {
        JwtUser user = this.kairosUserService.findUserByUsername(userName);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: No User found with username " + userName);
        }
        user.active(!user.getActive());
        this.kairosUserService.saveUser(user);
    }

}
