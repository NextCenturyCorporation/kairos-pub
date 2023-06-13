package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.Experiment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ExperimentRepository extends CrudRepository<Experiment, String> {
    List<Experiment> findAll();
    void deleteById(String idList);
}
