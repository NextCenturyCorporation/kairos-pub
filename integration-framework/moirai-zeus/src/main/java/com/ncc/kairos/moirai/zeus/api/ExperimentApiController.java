package com.ncc.kairos.moirai.zeus.api;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.ncc.kairos.moirai.zeus.model.CreateEnclaveRequest;
import com.ncc.kairos.moirai.zeus.model.Experiment;
import com.ncc.kairos.moirai.zeus.model.NodeTypeResponse;
import com.ncc.kairos.moirai.zeus.model.StringResponse;
import com.ncc.kairos.moirai.zeus.services.ExperimentServices;
import com.ncc.kairos.moirai.zeus.services.PropertiesService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.annotations.ApiParam;

@Controller
@RequestMapping("${openapi.moiraiZeus.base-path:}")
public class ExperimentApiController implements ExperimentApi {

    @Autowired
    ExperimentServices experimentService;

    @Autowired
    PropertiesService propertiesService;

    @Override
    public ResponseEntity<NodeTypeResponse> retrieveK8sNodeTypes() {
        return new ResponseEntity<NodeTypeResponse>(this.experimentService.getNodeTypes(), HttpStatus.OK);
    }

    
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Experiment>> retrieveExperiments() {
        return new ResponseEntity<List<Experiment>> (this.experimentService.getAllExperiments(), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> saveOrUpdateExperiment(@ApiParam(value = "experiment to save"  )  @Valid @RequestBody Experiment experiment) {
        this.experimentService.saveOrUpdateExperiment(experiment);
        return new ResponseEntity<>(new StringResponse().value("Experiment Saved/Updated."), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public  ResponseEntity<StringResponse> deleteExperiment(@NotNull @ApiParam(value = "Experiment id of Experiment to delete", required = true) @Valid @RequestParam(value = "id", required = true) String id) {
        this.experimentService.deleteExperiment(id);
        return new ResponseEntity<>(new StringResponse().value("Experiment Deleted."), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CREATE-ENCLAVE')")
    public ResponseEntity<StringResponse> createNodeGroups(
        @ApiParam(value = "Terminates clotho with the selected parameters"  )
        @Valid @RequestBody CreateEnclaveRequest createEnclaveRequest) {

        try {
            //this.experimentService.createNodeGroups(createEnclaveRequest.getExperimentId(), propertiesService.getRetryAttempts());
            this.experimentService.createExperimentRun(createEnclaveRequest.getExperimentId());
            return new ResponseEntity<>(new StringResponse().value("Copied enclave template and created enclave successfully."), HttpStatus.OK);
        }
        catch (ResponseStatusException e){
            throw e;
        }
        catch(Exception e) {
            return new ResponseEntity<>(new StringResponse().value("Unable to create enclave"), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CREATE-ENCLAVE')")
    public ResponseEntity<StringResponse> terminateNodeGroups(
        @ApiParam(value = "Terminates clotho with the selected parameters"  )
        @Valid @RequestBody CreateEnclaveRequest createEnclaveRequest) {
        try {
            this.experimentService.terminateNodeGroups(createEnclaveRequest.getExperimentId(), propertiesService.getRetryAttempts());
            return new ResponseEntity<>(new StringResponse().value("Created Nodegroup for experiment: id=" + createEnclaveRequest.getExperimentId()), HttpStatus.OK);
        }
        catch (ResponseStatusException e){
            throw e;
        }
        catch(Exception e) {
            return new ResponseEntity<>(new StringResponse().value("Unable to create node groups"), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ADMIN','TERMINATE-ENCLAVE')")
    public ResponseEntity<StringResponse> createK8sEnv() {
        try {
            this.experimentService.createK8sEnv(propertiesService.getRetryAttempts());
            return new ResponseEntity<>(new StringResponse().value("Archived enclave configuration."), HttpStatus.OK);
        }
        catch (ResponseStatusException e){
            throw e;
        }
        catch(Exception e) {
            return new ResponseEntity<>(new StringResponse().value("Unable to create K8s Environment Infrastructure."), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ADMIN','TERMINATE-ENCLAVE')")
    public ResponseEntity<StringResponse> terminateK8sEnv() {
        try {
            this.experimentService.terminateK8sEnv(propertiesService.getRetryAttempts());
            return new ResponseEntity<>(new StringResponse().value("Archived enclave configuration."), HttpStatus.OK);
        }
        catch (ResponseStatusException e){
            throw e;
        }
        catch(Exception e) {
            return new ResponseEntity<>(new StringResponse().value("Unable to destroy K8s Environment Infrastructure."), HttpStatus.BAD_REQUEST);
        }
    }

    
    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StringResponse> getK8sStatus() {
        return new ResponseEntity<>(new StringResponse().value(this.experimentService.getK8sStatus()), HttpStatus.OK);
    }

    
}
