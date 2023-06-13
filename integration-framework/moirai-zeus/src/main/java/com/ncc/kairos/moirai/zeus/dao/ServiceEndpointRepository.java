package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.ServiceEndpoint;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ServiceEndpointRepository links the application to the database.
 * Custom queries can be accomplished here for Service information.
 * @author will gossard
 * @version 0.1
 */
@Repository
public interface ServiceEndpointRepository extends CrudRepository<ServiceEndpoint, String> {

    /**
     * Get ServiceEndpoint by uri.
     */
    List<ServiceEndpoint> findByUri(String uri);
}
