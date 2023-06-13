package com.ncc.kairos.moirai.zeus.dao;

import java.util.List;

import com.ncc.kairos.moirai.zeus.model.FeatureFlagOverride;

import org.springframework.data.repository.CrudRepository;

public interface FeatureFlagOverrideRepository extends CrudRepository<FeatureFlagOverride, String> {
    List<FeatureFlagOverride> findAll();

    FeatureFlagOverride findByName(String name);
}
