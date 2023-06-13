package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.FeatureFlag;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends CrudRepository<FeatureFlag, String> {
    List<FeatureFlag> findAll();

    void deleteById(String id);

    FeatureFlag findByName(String name);

    Optional<FeatureFlag> findById(String id);
}
