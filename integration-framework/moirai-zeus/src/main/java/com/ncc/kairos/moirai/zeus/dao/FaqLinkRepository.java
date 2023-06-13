package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.FaqLink;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FaqLinkRepository extends CrudRepository<FaqLink, String> {
    List<FaqLink> findAll();
    List<FaqLink> findAllByFaqId(String faqId);
    void deleteByFaqId(String faqId);
    void deleteByFaqIdIn(List<String> faqIdList);
}
