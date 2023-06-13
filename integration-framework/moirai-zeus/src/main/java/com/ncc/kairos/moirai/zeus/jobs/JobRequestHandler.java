package com.ncc.kairos.moirai.zeus.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ncc.kairos.moirai.zeus.dao.JobRequestRepository;
import com.ncc.kairos.moirai.zeus.exceptions.UnaccessibleEndpointException;
import com.ncc.kairos.moirai.zeus.model.JobRequest;
import com.ncc.kairos.moirai.zeus.model.ValidationResponse;
import com.ncc.kairos.moirai.zeus.resources.JobRequestStatus;
import com.ncc.kairos.moirai.zeus.resources.JobRequestTypes;
import com.ncc.kairos.moirai.zeus.services.FilesService;
import com.ncc.kairos.moirai.zeus.services.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component
public class JobRequestHandler {

    @Autowired
    JobRequestRepository jobRequestRepository;

    @Autowired
    FilesService filesService;

    @Autowired
    ValidationService validationService;

    public static JobRequest makeJobRequest(JobRequestTypes requestType, int limit, Map<String, String> extraInformation) {
        JobRequest newRequest = new JobRequest();
        newRequest.setStatus(JobRequestStatus.REQUESTED.toString());
        newRequest.setAttempt(0);

        newRequest.setRequestType(requestType.toString());
        newRequest.setAttemptLimit(limit);
        newRequest.setExtraInformation(new Gson().toJson(extraInformation));

        return newRequest;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void validationJobs() throws Exception {
        List<JobRequest> requests = jobRequestRepository.findAllByRequestType(JobRequestTypes.VALIDATION.toString());
        requests = requests.stream().filter(request -> (request.getAttempt() < request.getAttemptLimit())).collect(Collectors.toList());
        requests = requests.stream().filter(request -> (!request.getStatus().equals(JobRequestStatus.COMPLETED.toString()))).collect(Collectors.toList());
        requests = requests.stream().filter(request -> (!request.getStatus().equals(JobRequestStatus.FAILED_FINAL.toString()))).collect(Collectors.toList());
        for (JobRequest request : requests) {
            if (!request.getStatus().equals(JobRequestStatus.COMPLETED.toString())
                    && !request.getStatus().equals(JobRequestStatus.FAILED_FINAL.toString())
                    && request.getAttempt() < request.getAttemptLimit()) {

                // Set request as running
                request.setAttempt(request.getAttempt() + 1);
                request.setStatus(JobRequestStatus.RUNNING.toString());
                jobRequestRepository.save(request);

                // Examine request

                Type stringMapType = new TypeToken<Map<String, String>>() { }.getType();
                Map<String, String> data = new Gson().fromJson(request.getExtraInformation(), stringMapType);
                String fileId = data.get("fileId");

                // Get original file
                byte[] fileData = null;
                try {
                    fileData = filesService.downloadFile(fileId);
                } catch (NoSuchElementException e) {
                    jobRequestRepository.delete(request);
                }

                if (fileData != null) {
                    ValidationResponse validationOutput;
                    try {
                        validationOutput = validationService.runValidation(fileData);
                    } catch (UnaccessibleEndpointException e) {
                        validationOutput = null;
                    }

                    // Update Job request record
                    if (validationOutput == null) {
                        if (request.getAttempt() < request.getAttemptLimit()) {
                            request.setStatus(JobRequestStatus.FAILED.toString());
                        } else {
                            request.setStatus(JobRequestStatus.FAILED_FINAL.toString());
                            filesService.setValidationDone(fileId);
                        }
                    } else {
                        try {
                            filesService.addValidation(fileId, validationOutput);
                            request.setStatus(JobRequestStatus.COMPLETED.toString());
                        } catch (JsonProcessingException jpe) {
                            request.setStatus(JobRequestStatus.FAILED.toString());
                        }
                    }
                    jobRequestRepository.save(request);
                }
            }
        }
    }
}
