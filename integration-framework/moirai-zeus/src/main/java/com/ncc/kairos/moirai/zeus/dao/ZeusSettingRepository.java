package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.ZeusSetting;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ZeusSettingRepository extends CrudRepository<ZeusSetting, String> {
    List<ZeusSetting> findAll();
    ZeusSetting findByName(String name);
    void deleteById(String id);
}
