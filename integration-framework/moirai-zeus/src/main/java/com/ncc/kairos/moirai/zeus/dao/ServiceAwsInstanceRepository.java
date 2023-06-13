package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.ServiceAwsInstance;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ServiceAwsInstanceRepository links the application to the database.
 * Custom queries can be accomplished here for Service information.
 * @author will gossard
 * @version 0.1
 */
@Repository
public interface ServiceAwsInstanceRepository extends CrudRepository<ServiceAwsInstance, String> {

    /**
     * Get ServiceAwsInstance by aws instanceId.
     */
    List<ServiceAwsInstance> findByInstanceId(String instanceId);
}
