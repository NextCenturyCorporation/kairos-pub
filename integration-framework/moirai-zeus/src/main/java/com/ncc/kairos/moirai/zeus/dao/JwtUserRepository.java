package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.JwtUser;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * JwtUserRepository links the application to the database.
 * Custom querries can be accomplished here for JwtUser information.
 * @author will gossard
 * @version 0.1
 */
@Repository
public interface JwtUserRepository extends CrudRepository<JwtUser, String> {

    JwtUser findByUsername(String username);

    JwtUser findByEmailAddress(String email);

    List<JwtUser> findByTeamName(String teamName);
}
