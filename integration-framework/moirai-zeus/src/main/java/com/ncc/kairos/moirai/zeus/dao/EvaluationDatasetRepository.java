package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.Dataset;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationDatasetRepository extends CrudRepository<Dataset, String> {
    List<Dataset> findAll();

    Optional<Dataset> findById(String id);

    void deleteById(String id);
}
