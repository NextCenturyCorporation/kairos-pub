package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.ContactRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * ContactRequestRepository links the application to the database.
 * Custom queries can be accomplished here for ContactRequestRepository information.
 * @author ryan scott
 * @version 0.1
 */
@Repository
public interface ContactRequestRepository extends CrudRepository<ContactRequest, String> {

    List<ContactRequest> findByStatus(String status);
}
