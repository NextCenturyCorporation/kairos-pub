package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.JwtRole;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Used to Manage the JwtRole, used to group JwtPermissions.
 *
 * @author larry mize
 * @version 0.1
 */
@Repository
public interface JwtRoleRepository extends CrudRepository<JwtRole, String> {

    JwtRole findByName(String name);

    List<JwtRole> findAll();

}
