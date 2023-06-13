package com.ncc.kairos.moirai.zeus.dao;

import org.springframework.stereotype.Repository;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.DockerRegistry;
import org.springframework.data.repository.CrudRepository;

/**
 * Custom querries can be accomplished here for Docker Authentication information.
 * 
 * @author will gossard
 * @version 0.1
 */
@Repository
public interface DockerRegistryRepository extends CrudRepository<DockerRegistry, String> {
    
    DockerRegistry findByOwner(String owner);

    List<DockerRegistry> findAll();
}
