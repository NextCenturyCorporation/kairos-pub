package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.ContactRequestRepository;
import com.ncc.kairos.moirai.zeus.model.ContactRequest;
import com.ncc.kairos.moirai.zeus.model.ContactRequestMessage;
import com.ncc.kairos.moirai.zeus.resources.ContactStatus;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing contact requests.
 *
 * @author ryan scott
 * @version 0.1
 */
@Service
public class ContactRequestService {

    @Autowired
    private ContactRequestRepository repository;

    /**
     * Updates an existing jwtUser.
     *
     * @param contactRequest The new ContactRequest.
     */
    public void saveNewRequest(ContactRequest contactRequest) {
        if (contactRequest.getDatetime() == null) {
            OffsetDateTime curTime = OffsetDateTime.now();
            contactRequest.setDatetime(curTime);
            for (ContactRequestMessage message : contactRequest.getMessages()) {
                message.setDatetime(curTime);
            }
        }
        contactRequest.setStatus(ContactStatus.UNANSWERED.toString());
        repository.save(contactRequest);
    }

    /**
     *  Returns all contact requests.
     *
     * @return
     */
    public List<ContactRequest> findAllContactRequests() {
        List<ContactRequest> requestList = new ArrayList<>();
        Iterable<ContactRequest> it = repository.findAll();

        for (ContactRequest request : it) {
            requestList.add(request);
        }
        return requestList;
    }

    public List<ContactRequest> findAllByStatus(String status) {
        List<ContactRequest> requestList = new ArrayList<>();
        if (StringUtils.isEmpty(status)) {
            requestList.addAll(findAllContactRequests());
        } else {
            requestList.addAll(this.repository.findByStatus(status));
        }
        // Non-empty status throw Not_FOund error
        if (requestList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Contacts found for Status: " + status);
        }
        return requestList;
    }
}
