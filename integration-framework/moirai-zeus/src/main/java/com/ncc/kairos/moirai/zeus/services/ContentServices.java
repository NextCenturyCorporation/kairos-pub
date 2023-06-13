package com.ncc.kairos.moirai.zeus.services;

import java.util.ArrayList;
import java.util.List;

import com.ncc.kairos.moirai.zeus.dao.DropdownDaoRepository;
import com.ncc.kairos.moirai.zeus.model.DropdownDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing Content requests.
 *
 * @author Baileys in a shoe
 * @version 0.1
 */
@Service
public class ContentServices {

    @Autowired
    private DropdownDaoRepository repository;

    public List<DropdownDao> getDropdownsByKey(String key, Boolean selectOneOption) {
        List<DropdownDao> resultList = new ArrayList<>();
        if (Boolean.TRUE.equals(selectOneOption)) {
            resultList.addAll(repository.findAllByLookupIdOrderByDisplayOrder("select.one"));
        }
        List<DropdownDao> results = repository.findAllByLookupIdOrderByDisplayOrder(key);
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: Could not find any results from given key   .");
        }
        resultList.addAll(results);
        return resultList;
    }

    
}
