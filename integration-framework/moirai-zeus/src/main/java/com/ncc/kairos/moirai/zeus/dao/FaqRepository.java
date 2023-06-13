package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.Faq;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FaqRepository extends CrudRepository<Faq, String> {
    List<Faq> findAll();
    void deleteByIdIn(List<String> idList);
}
