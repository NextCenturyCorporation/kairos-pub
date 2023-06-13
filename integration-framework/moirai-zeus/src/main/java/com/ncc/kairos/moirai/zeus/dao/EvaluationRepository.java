package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.Evaluation;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends CrudRepository<Evaluation, String> {
    List<Evaluation> findAll();

    void deleteById(String id);

    Optional<Evaluation> findByName(String name);

    Optional<Evaluation> findById(String id);
}
