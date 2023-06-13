package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.JwtPermission;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing JwtPermissions, used to manage what endpoints the user can and can not access.
 *
 * @author larry mize
 * @version 0.1
 */
@Repository
public interface JwtPermissionRepository extends CrudRepository<JwtPermission, String> {

    JwtPermission findByName(String name);

    List<JwtPermission> findAll();

}
