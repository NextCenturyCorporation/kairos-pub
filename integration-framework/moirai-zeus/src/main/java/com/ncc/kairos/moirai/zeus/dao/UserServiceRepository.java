package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.Service;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * JwtServiceRepository links the application to the database.
 * Custom querries can be accomplished here for Service information.
 * @author will gossard
 * @version 0.1
 */
@Repository
public interface UserServiceRepository extends CrudRepository<Service, String> {

    /**
     * Get Service by access level.
     */
    List<Service> findByAccess(String access);

    /**
     * From the database's list of services return those that match the name and access.
     * @param username of Services to find
     * @param access of Services to find
     * @return found Service List
     */
    List<Service> findByNameAndAccess(String username, String access);

    /**
     * Get service by name.
     * @param Name - name of the service
     * @return
     */
    List<Service> findByName(String name);

    /**
     * Grab all of the services in the database.
     * @return All services in database
     */
    List<Service> findAll();

    /**
     * Grab all Services associated with a Team.
     */
    List<Service> findAllByTeamName(String teamName);

}
