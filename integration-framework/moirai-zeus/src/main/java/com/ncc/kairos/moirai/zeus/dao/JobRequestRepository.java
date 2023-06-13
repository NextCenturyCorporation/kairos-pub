package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.JobRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface JobRequestRepository extends CrudRepository<JobRequest, String> {
    List<JobRequest> findAll();
    List<JobRequest> findAllByRequestType(String requestType);
    void deleteById(String id);
}
