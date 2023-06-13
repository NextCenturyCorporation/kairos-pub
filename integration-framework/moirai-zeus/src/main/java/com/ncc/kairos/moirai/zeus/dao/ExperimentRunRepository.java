package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.ExperimentRun;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ExperimentRunRepository extends CrudRepository<ExperimentRun, String> {
    List<ExperimentRun> findAll();

    void deleteById(String id);

    List<ExperimentRun> findAllByStatus(String status);

    Optional<ExperimentRun> findById(String id);
}
